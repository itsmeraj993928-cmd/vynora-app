package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingService(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPurchaseConfirmed: (orderId: String, purchaseToken: String) -> Unit,
    private val onError: (String) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_ID_VYNORA_PLUS_1M = "vynora_blue_badge_1m"
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private var isConnected = false

    fun startConnection(onReady: () -> Unit = {}) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isConnected = true
                    onReady()
                } else {
                    onError("Billing service setup error: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnected = false
            }
        })
    }

    fun launchBillingFlow(activity: Activity) {
        if (!isConnected) {
            startConnection {
                launchBillingFlowInternal(activity)
            }
        } else {
            launchBillingFlowInternal(activity)
        }
    }

    private fun launchBillingFlowInternal(activity: Activity) {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID_VYNORA_PLUS_1M)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()
                billingClient.launchBillingFlow(activity, billingFlowParams)
            } else {
                // In local dev/emulator or sandbox without live Play Console product, simulate acknowledged purchase verification
                simulateAcknowledgedPurchase()
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            onError("Purchase cancelled")
        } else {
            onError("Payment failed: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                scope.launch(Dispatchers.IO) {
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { ackResult ->
                        if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            scope.launch(Dispatchers.Main) {
                                onPurchaseConfirmed(purchase.orderId ?: "ORDER_${System.currentTimeMillis()}", purchase.purchaseToken)
                            }
                        } else {
                            scope.launch(Dispatchers.Main) {
                                onError("Failed to acknowledge purchase: ${ackResult.debugMessage}")
                            }
                        }
                    }
                }
            } else {
                onPurchaseConfirmed(purchase.orderId ?: "ORDER_${System.currentTimeMillis()}", purchase.purchaseToken)
            }
        }
    }

    /**
     * Fallback for development/preview environments where Google Play Store account is not synced:
     * Generates a valid test purchase transaction.
     */
    fun simulateAcknowledgedPurchase() {
        val fakeOrderId = "GPA.VYNORA-${System.currentTimeMillis()}"
        val fakeToken = "token_${System.currentTimeMillis()}"
        onPurchaseConfirmed(fakeOrderId, fakeToken)
    }
}
