/* Health Monitor — shared frontend helpers (no framework). */
(function () {
  const TOKEN_KEY = "hm_token";
  const ROLE_KEY = "hm_role";
  const NAME_KEY = "hm_name";
  const USER_KEY = "hm_user";

  function loginUrl() {
    const ret = encodeURIComponent(location.pathname + location.search);
    return "/login.html?returnTo=" + ret;
  }

  function safeReturnTo(value) {
    return value && value.startsWith("/") && !value.startsWith("//") ? value : "/dashboard.html";
  }

  const HM = {
    token: () => localStorage.getItem(TOKEN_KEY),
    role: () => localStorage.getItem(ROLE_KEY),
    name: () => localStorage.getItem(NAME_KEY),
    username: () => localStorage.getItem(USER_KEY),

    save(session) {
      localStorage.setItem(TOKEN_KEY, session.token);
      localStorage.setItem(ROLE_KEY, session.role);
      localStorage.setItem(NAME_KEY, session.name || session.username || "");
      localStorage.setItem(USER_KEY, session.username || "");
    },

    clear() {
      [TOKEN_KEY, ROLE_KEY, NAME_KEY, USER_KEY].forEach((k) => localStorage.removeItem(k));
    },

    logout() {
      HM.clear();
      location.href = "/login.html";
    },

    isDoctor: () => HM.role() === "DOCTOR",

    /** Every page except login calls this first. */
    requireLogin() {
      if (!HM.token()) location.replace(loginUrl());
    },

    /** fetch() with the JWT attached; redirects to login on 401. */
    async api(url, options = {}) {
      const headers = Object.assign({}, options.headers);
      const token = HM.token();
      if (token) headers["Authorization"] = "Bearer " + token;
      if (options.body && typeof options.body === "string" && !headers["Content-Type"]) {
        headers["Content-Type"] = "application/json";
      }
      const res = await fetch(url, Object.assign({}, options, { headers }));
      if (res.status === 401) {
        HM.clear();
        location.replace(loginUrl());
        throw new Error("unauthorized");
      }
      if (!res.ok) {
        let message = res.status + " " + res.statusText;
        try {
          const body = await res.json();
          if (body && body.error) message = body.error;
        } catch (e) { /* keep default message */ }
        const err = new Error(message);
        err.status = res.status;
        throw err;
      }
      if (res.status === 204) return null;
      const contentType = res.headers.get("content-type") || "";
      return contentType.includes("application/json") ? res.json() : res.text();
    },

    /** Render the sticky top navigation into #nav. */
    renderNav(active) {
      const host = document.getElementById("nav");
      if (!host) return;
      const links = [
        ["dashboard", "/dashboard.html", "Dashboard"],
        ["patients", "/patients.html", "Patients"],
        ["alerts", "/index.html", "Live Alerts"],
        ["api", "/swagger-ui.html", "API Docs"],
      ];
      const html =
        '<a class="brand" href="/dashboard.html"><span class="pulse"></span>Health Monitor</a>' +
        '<nav class="nav-links">' +
        links
          .map(
            ([key, href, label]) =>
              '<a href="' + href + '"' + (key === active ? ' class="active"' : "") +
              (key === "api" ? ' target="_blank"' : "") + ">" + label + "</a>"
          )
          .join("") +
        "</nav>" +
        '<div class="userbox"><div class="who"><b>' +
        escapeHtml(HM.name() || HM.username() || "") +
        "</b><span>" +
        escapeHtml(HM.role() || "") +
        '</span></div><button class="btn small" onclick="HM.logout()">Sign out</button></div>';
      host.innerHTML = html;
    },

    /** Colour class for a value against thresholds: ok / warn / bad. */
    statusClass(value, min, max, guard) {
      if (value < min || value > max) return "bad";
      if (value <= min + guard || value >= max - guard) return "warn";
      return "ok";
    },

    loginUrl,
    safeReturnTo,
  };

  function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
  }

  window.HM = HM;
  window.escapeHtml = escapeHtml;
})();
