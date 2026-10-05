const http = require('http');

const PORT = process.env.PORT || 3000;

const htmlContent = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>VYNORA - Sign In</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
    body {
      background: #050509;
      color: #fff;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      padding: 16px;
    }
    .device-wrapper {
      width: 100%;
      max-width: 420px;
      background: #090B14;
      border: 1px solid rgba(0, 209, 255, 0.25);
      border-radius: 36px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.8), 0 0 40px rgba(255, 46, 147, 0.15);
      overflow: hidden;
      display: flex;
      flex-direction: column;
      position: relative;
    }
    .status-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 14px 24px 8px;
      font-size: 13px;
      font-weight: 600;
      color: #94A3B8;
    }
    .app-header {
      text-align: center;
      padding: 24px 20px 16px;
    }
    .logo-container {
      width: 68px;
      height: 68px;
      border-radius: 20px;
      background: linear-gradient(135deg, #FF2E93 0%, #00D1FF 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 12px;
      box-shadow: 0 0 25px rgba(255, 46, 147, 0.5);
    }
    .logo-v {
      font-size: 38px;
      font-weight: 900;
      color: #fff;
      font-style: italic;
    }
    h1 {
      font-size: 28px;
      font-weight: 900;
      letter-spacing: 5px;
      background: linear-gradient(90deg, #FFFFFF, #E2E8F0);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      margin-bottom: 4px;
    }
    .tagline {
      font-size: 13px;
      color: #00D1FF;
      font-weight: 600;
      letter-spacing: 0.5px;
    }
    .pkg-badge {
      display: inline-block;
      margin-top: 8px;
      padding: 3px 10px;
      background: rgba(0, 209, 255, 0.1);
      border: 1px solid rgba(0, 209, 255, 0.3);
      border-radius: 12px;
      font-size: 11px;
      color: #00D1FF;
      font-family: monospace;
    }
    .content-card {
      background: #0F121E;
      border-radius: 28px 28px 0 0;
      border-top: 1px solid rgba(0, 209, 255, 0.2);
      padding: 24px 20px 36px;
      flex: 1;
    }
    .tab-bar {
      display: flex;
      background: #090B14;
      border-radius: 14px;
      padding: 4px;
      margin-bottom: 22px;
      border: 1px solid rgba(255, 255, 255, 0.08);
    }
    .tab-btn {
      flex: 1;
      padding: 10px;
      text-align: center;
      font-size: 13.5px;
      font-weight: 700;
      border-radius: 10px;
      cursor: pointer;
      transition: all 0.2s ease;
      color: #94A3B8;
      border: none;
      background: transparent;
    }
    .tab-btn.active {
      background: linear-gradient(135deg, #FF2E93, #D91B74);
      color: #fff;
      box-shadow: 0 4px 15px rgba(255, 46, 147, 0.35);
    }
    .field-group {
      margin-bottom: 16px;
    }
    .field-label {
      display: block;
      font-size: 12px;
      font-weight: 600;
      color: #CBD5E1;
      margin-bottom: 6px;
    }
    .input-field {
      width: 100%;
      background: #161928;
      border: 1px solid rgba(255, 255, 255, 0.12);
      border-radius: 14px;
      padding: 13px 16px;
      color: #fff;
      font-size: 14px;
      outline: none;
      transition: border-color 0.2s;
    }
    .input-field:focus {
      border-color: #00D1FF;
      box-shadow: 0 0 10px rgba(0, 209, 255, 0.2);
    }
    .input-field::placeholder {
      color: #64748B;
    }
    .forgot-link {
      display: block;
      text-align: right;
      color: #00D1FF;
      font-size: 12px;
      font-weight: 600;
      margin-top: 6px;
      text-decoration: none;
      cursor: pointer;
    }
    .forgot-link:hover { text-decoration: underline; }
    .btn-primary {
      width: 100%;
      padding: 14px;
      border-radius: 14px;
      background: linear-gradient(135deg, #FF2E93, #C71585);
      border: none;
      color: #fff;
      font-size: 15px;
      font-weight: 700;
      cursor: pointer;
      box-shadow: 0 6px 20px rgba(255, 46, 147, 0.4);
      margin-top: 14px;
      transition: transform 0.1s, opacity 0.2s;
    }
    .btn-primary:active { transform: scale(0.98); }
    .divider {
      display: flex;
      align-items: center;
      margin: 20px 0;
      color: #64748B;
      font-size: 12px;
    }
    .divider::before, .divider::after {
      content: '';
      flex: 1;
      height: 1px;
      background: rgba(255, 255, 255, 0.1);
    }
    .divider span { padding: 0 12px; }
    .btn-google {
      width: 100%;
      padding: 12px;
      border-radius: 14px;
      background: #1A1D2E;
      border: 1px solid rgba(255, 255, 255, 0.15);
      color: #fff;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      transition: background 0.2s;
    }
    .btn-google:hover { background: #22263C; }
    .lang-row {
      display: flex;
      justify-content: center;
      gap: 16px;
      margin-top: 24px;
    }
    .lang-btn {
      background: transparent;
      border: 1px solid rgba(255, 255, 255, 0.15);
      color: #94A3B8;
      border-radius: 8px;
      padding: 5px 12px;
      font-size: 11px;
      cursor: pointer;
    }
    .lang-btn.active {
      color: #00D1FF;
      border-color: #00D1FF;
    }
    .toast {
      position: absolute;
      top: 60px;
      left: 50%;
      transform: translateX(-50%);
      background: rgba(15, 23, 42, 0.95);
      border: 1px solid #00D1FF;
      color: #fff;
      padding: 10px 20px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
      box-shadow: 0 10px 30px rgba(0,0,0,0.5);
      display: none;
      z-index: 10;
    }
  </style>
</head>
<body>

  <div class="device-wrapper">
    <div id="toast" class="toast">Welcome to VYNORA!</div>

    <div class="status-bar">
      <span>09:41</span>
      <span>5G &nbsp; 100%</span>
    </div>

    <div class="app-header">
      <div class="logo-container">
        <span class="logo-v">V</span>
      </div>
      <h1>VYNORA</h1>
      <div class="tagline">Share your world.</div>
      <div class="pkg-badge">com.aistudio.vynorasocial.wrtkpv</div>
    </div>

    <div class="content-card">
      <div class="tab-bar">
        <button id="tabSignIn" class="tab-btn active" onclick="switchTab(false)">Sign In</button>
        <button id="tabSignUp" class="tab-btn" onclick="switchTab(true)">Create Account</button>
      </div>

      <div id="signInForm">
        <button class="btn-google" onclick="handleGoogleSignIn()">
          <svg width="20" height="20" viewBox="0 0 24 24">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22 0.81-.63z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
          </svg>
          Sign in with Google
        </button>

        <div class="divider">
          <span>OR SIGN IN WITH EMAIL</span>
        </div>

        <div class="field-group">
          <label class="field-label">Email or Username</label>
          <input type="text" id="loginEmail" class="input-field" placeholder="Enter your email or handle" autofocus />
        </div>

        <div class="field-group">
          <label class="field-label">Password</label>
          <input type="password" id="loginPassword" class="input-field" placeholder="••••••••" />
          <a class="forgot-link" onclick="showToast('Password reset link sent to your email!')">Forgot password?</a>
        </div>

        <button class="btn-primary" onclick="handleSignIn()">Sign In</button>
      </div>

      <div id="signUpForm" style="display: none;">
        <div class="field-group">
          <label class="field-label">Email</label>
          <input type="email" id="regEmail" class="input-field" placeholder="name@domain.com" />
        </div>
        <div class="field-group">
          <label class="field-label">Password</label>
          <input type="password" id="regPassword" class="input-field" placeholder="Create password (8+ chars)" />
        </div>
        <div class="field-group">
          <label class="field-label">Referral Code (Optional)</label>
          <input type="text" id="regReferral" class="input-field" placeholder="e.g. ALEX42" />
        </div>
        <button class="btn-primary" onclick="handleSignUp()">Continue</button>
      </div>

      <div class="lang-row">
        <button class="lang-btn active" onclick="setLang('en', this)">English</button>
        <button class="lang-btn" onclick="setLang('hi', this)">हिन्दी</button>
        <button class="lang-btn" onclick="setLang('ur', this)">اردو</button>
      </div>
    </div>
  </div>

  <script>
    function switchTab(isSignUp) {
      document.getElementById('tabSignIn').classList.toggle('active', !isSignUp);
      document.getElementById('tabSignUp').classList.toggle('active', isSignUp);
      document.getElementById('signInForm').style.display = isSignUp ? 'none' : 'block';
      document.getElementById('signUpForm').style.display = isSignUp ? 'block' : 'none';
    }

    function showToast(msg) {
      const toast = document.getElementById('toast');
      toast.innerText = msg;
      toast.style.display = 'block';
      setTimeout(() => { toast.style.display = 'none'; }, 2600);
    }

    function handleSignIn() {
      const email = document.getElementById('loginEmail').value.trim();
      if (!email) {
        showToast('Please enter your email or username');
        return;
      }
      showToast('Signing in to @' + email + '...');
    }

    function handleSignUp() {
      const email = document.getElementById('regEmail').value.trim();
      if (!email) {
        showToast('Please enter your email to proceed');
        return;
      }
      showToast('Proceeding to Step 2: Profile setup...');
    }

    function handleGoogleSignIn() {
      showToast('Connecting to Google Identity Service...');
    }

    function setLang(code, btn) {
      document.querySelectorAll('.lang-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      if (code === 'hi') {
        document.querySelector('.tagline').innerText = 'अपनी दुनिया साझा करें।';
        document.getElementById('tabSignIn').innerText = 'साइन इन';
        document.getElementById('tabSignUp').innerText = 'खाता बनाएं';
      } else if (code === 'ur') {
        document.querySelector('.tagline').innerText = 'اپنی دنیا شیئر کریں۔';
        document.getElementById('tabSignIn').innerText = 'سائن ان';
        document.getElementById('tabSignUp').innerText = 'اکاؤنٹ بنائیں';
      } else {
        document.querySelector('.tagline').innerText = 'Share your world.';
        document.getElementById('tabSignIn').innerText = 'Sign In';
        document.getElementById('tabSignUp').innerText = 'Create Account';
      }
      showToast('Language updated: ' + code.toUpperCase());
    }
  </script>
</body>
</html>`;

const server = http.createServer((req, res) => {
  res.writeHead(200, {
    'Content-Type': 'text/html; charset=utf-8',
    'X-App-Package': 'com.aistudio.vynorasocial.wrtkpv',
    'X-App-Name': 'VYNORA'
  });
  res.end(htmlContent);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`VYNORA preview server running on port ${PORT}`);
});
