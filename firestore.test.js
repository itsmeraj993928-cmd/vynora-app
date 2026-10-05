const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: emulatorHost,
      port: emulatorPort,
      rules: rules,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read users collection", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user can create their own profile document with clean defaults", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      id: ALICE_UID,
      email: "alice@example.com",
      username: "alice_v",
      fullName: "Alice Vance",
      badgeActive: false,
      badgeExpiry: 0,
      badgeSource: "",
      referralCount: 0,
      role: "USER",
      isOfficial: false,
      isBanned: false,
    })
  );
});

test("Normal user cannot self-grant badgeActive or change admin role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      id: ALICE_UID,
      email: "alice@example.com",
      username: "alice_v",
      badgeActive: true,
      badgeExpiry: Date.now() + 1000000,
      badgeSource: "paid",
      referralCount: 10,
      role: "ADMIN",
      isOfficial: true,
      isBanned: false,
    })
  );
});

test("Authenticated user can create post with their authorId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      caption: "Hello VYNORA!",
      likesCount: 0,
      createdAt: Date.now(),
    })
  );
});

test("User cannot create post with spoofed authorId", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("posts").doc("post_2").set({
      authorId: ALICE_UID,
      caption: "Spoofed post",
      likesCount: 0,
      createdAt: Date.now(),
    })
  );
});
