/* Jellyarr – Nuvio provider runtime.
 * Runs Nuvio-format scrapers (module.exports = { getStreams }) inside a hidden WebView.
 * Network goes through the native bridge (OkHttp), so there are no CORS restrictions. */
(function () {
  'use strict';
  var pending = {};
  var seq = 0;

  window.__nativeResponse = function (id, res) {
    var p = pending[id];
    if (!p) return;
    delete pending[id];
    if (res && res.error) p.reject(new Error(res.error));
    else p.resolve(res);
  };

  function headersToObject(h) {
    var o = {};
    if (!h) return o;
    if (typeof Headers !== 'undefined' && h instanceof Headers) { h.forEach(function (v, k) { o[k] = v; }); return o; }
    if (Array.isArray(h)) { h.forEach(function (p) { o[p[0]] = p[1]; }); return o; }
    for (var k in h) if (h[k] != null) o[k] = String(h[k]);
    return o;
  }

  function rawRequest(url, opts) {
    opts = opts || {};
    return new Promise(function (resolve, reject) {
      var id = 'r' + (++seq);
      pending[id] = { resolve: resolve, reject: reject };
      var body = opts.body;
      if (body != null && typeof body !== 'string') {
        if (typeof URLSearchParams !== 'undefined' && body instanceof URLSearchParams) body = body.toString();
        else body = JSON.stringify(body);
      }
      NativeBridge.request(id, String(url), String(opts.method || 'GET').toUpperCase(),
        JSON.stringify(headersToObject(opts.headers)), body == null ? '' : String(body),
        opts.redirect === 'manual' ? 'manual' : 'follow');
    });
  }

  function makeHeaders(raw) {
    var lower = {};
    for (var k in (raw || {})) lower[k.toLowerCase()] = raw[k];
    return {
      get: function (k) { var v = lower[String(k).toLowerCase()]; return v == null ? null : v; },
      has: function (k) { return String(k).toLowerCase() in lower; },
      forEach: function (cb) { for (var k in lower) cb(lower[k], k); },
      entries: function () { return Object.keys(lower).map(function (k) { return [k, lower[k]]; })[Symbol.iterator](); },
      raw: function () { return lower; }
    };
  }

  function makeResponse(r) {
    return {
      ok: r.status >= 200 && r.status < 300,
      status: r.status,
      statusText: r.statusText || '',
      url: r.url,
      redirected: !!r.redirected,
      headers: makeHeaders(r.headers),
      text: function () { return Promise.resolve(r.body); },
      json: function () { try { return Promise.resolve(JSON.parse(r.body)); } catch (e) { return Promise.reject(e); } },
      arrayBuffer: function () { return Promise.resolve(new TextEncoder().encode(r.body).buffer); },
      clone: function () { return makeResponse(r); }
    };
  }

  window.fetch = function (input, opts) {
    var url = input;
    if (input && typeof input === 'object' && input.url) { opts = opts || input; url = input.url; }
    return rawRequest(url, opts).then(makeResponse);
  };

  // ---------- axios ----------
  function makeAxios(defaults) {
    defaults = defaults || {};
    function req(config) {
      config = Object.assign({}, defaults, config || {});
      config.headers = Object.assign({}, defaults.headers || {}, (config && config.headers) || {});
      var url = config.url || '';
      if (config.baseURL && !/^https?:/i.test(url)) url = config.baseURL.replace(/\/$/, '') + '/' + url.replace(/^\//, '');
      if (config.params) {
        var qs = Object.keys(config.params).filter(function (k) { return config.params[k] != null; })
          .map(function (k) { return encodeURIComponent(k) + '=' + encodeURIComponent(config.params[k]); }).join('&');
        if (qs) url += (url.indexOf('?') >= 0 ? '&' : '?') + qs;
      }
      var data = config.data;
      var headers = config.headers;
      if (data != null && typeof data === 'object' && !(typeof URLSearchParams !== 'undefined' && data instanceof URLSearchParams)) {
        data = JSON.stringify(data);
        if (!headers['Content-Type'] && !headers['content-type']) headers['Content-Type'] = 'application/json';
      }
      return rawRequest(url, { method: config.method || 'GET', headers: headers, body: data,
        redirect: config.maxRedirects === 0 ? 'manual' : 'follow' }).then(function (r) {
        var d = r.body;
        if (config.responseType !== 'text') { try { d = JSON.parse(r.body); } catch (e) { /* keep text */ } }
        var lowerHeaders = {};
        for (var k in (r.headers || {})) lowerHeaders[k.toLowerCase()] = r.headers[k];
        var resp = { data: d, status: r.status, statusText: r.statusText || '', headers: lowerHeaders, config: config,
          request: { responseURL: r.url, res: { responseUrl: r.url } } };
        var valid = config.validateStatus || function (s) { return s >= 200 && s < 300; };
        if (!valid(r.status)) {
          var err = new Error('Request failed with status code ' + r.status);
          err.response = resp; err.config = config;
          throw err;
        }
        return resp;
      });
    }
    var ax = function (a, b) { return typeof a === 'string' ? req(Object.assign({}, b || {}, { url: a })) : req(a); };
    ax.request = req;
    ['get', 'delete', 'head', 'options'].forEach(function (m) {
      ax[m] = function (u, c) { return req(Object.assign({}, c || {}, { url: u, method: m.toUpperCase() })); };
    });
    ['post', 'put', 'patch'].forEach(function (m) {
      ax[m] = function (u, d, c) { return req(Object.assign({}, c || {}, { url: u, method: m.toUpperCase(), data: d })); };
    });
    ax.create = function (d) { return makeAxios(Object.assign({}, defaults, d || {})); };
    ax.defaults = defaults;
    ax.interceptors = { request: { use: function () {} }, response: { use: function () {} } };
    ax.isAxiosError = function (e) { return !!(e && e.config); };
    ax.default = ax;
    return ax;
  }
  window.axios = makeAxios({});

  // ---------- cheerio (DOMParser based subset) ----------
  function load(html) {
    var doc = new DOMParser().parseFromString(String(html == null ? '' : html), 'text/html');
    var proto = {};
    function wrap(nodes) {
      var w = Object.create(proto);
      w.nodes = nodes || [];
      w.length = w.nodes.length;
      for (var i = 0; i < w.nodes.length; i++) w[i] = w.nodes[i];
      return w;
    }
    function qsa(root, sel) {
      try { return Array.prototype.slice.call(root.querySelectorAll(sel)); } catch (e) { return []; }
    }
    function uniq(arr) { var out = []; arr.forEach(function (x) { if (x && out.indexOf(x) < 0) out.push(x); }); return out; }
    function matches(n, sel) { try { return n.nodeType === 1 && n.matches(sel); } catch (e) { return false; } }
    proto.find = function (sel) {
      var out = [];
      this.nodes.forEach(function (n) { if (n && n.querySelectorAll) out = out.concat(qsa(n, sel)); });
      return wrap(uniq(out));
    };
    proto.each = function (fn) {
      for (var i = 0; i < this.nodes.length; i++) if (fn.call(this.nodes[i], i, this.nodes[i]) === false) break;
      return this;
    };
    proto.map = function (fn) {
      var out = [];
      for (var i = 0; i < this.nodes.length; i++) {
        var v = fn.call(this.nodes[i], i, this.nodes[i]);
        if (v != null) out = out.concat(Array.isArray(v) ? v : [v]);
      }
      return wrap(out);
    };
    proto.get = function (i) { return i == null ? this.nodes.slice() : this.nodes[i < 0 ? this.nodes.length + i : i]; };
    proto.toArray = function () { return this.nodes.slice(); };
    proto.first = function () { return wrap(this.nodes.slice(0, 1)); };
    proto.last = function () { return wrap(this.nodes.slice(-1)); };
    proto.eq = function (i) { var n = this.nodes[i < 0 ? this.nodes.length + i : i]; return wrap(n ? [n] : []); };
    proto.slice = function (a, b) { return wrap(this.nodes.slice(a, b)); };
    proto.attr = function (name, value) {
      if (value !== undefined) { this.nodes.forEach(function (n) { if (n.setAttribute) n.setAttribute(name, value); }); return this; }
      var n = this.nodes[0];
      if (!n || !n.getAttribute) return undefined;
      var v = n.getAttribute(name);
      return v == null ? undefined : v;
    };
    proto.data = function (name) { return name ? this.attr('data-' + name) : undefined; };
    proto.val = function () { var n = this.nodes[0]; return n ? n.value : undefined; };
    proto.text = function () { return this.nodes.map(function (n) { return n && n.textContent != null ? n.textContent : ''; }).join(''); };
    proto.html = function () { var n = this.nodes[0]; return n ? (n.innerHTML != null ? n.innerHTML : null) : null; };
    proto.toString = function () { return this.nodes.map(function (n) { return n.outerHTML || ''; }).join(''); };
    proto.parent = function () { return wrap(uniq(this.nodes.map(function (n) { return n.parentElement; }))); };
    proto.parents = function (sel) {
      var out = [];
      this.nodes.forEach(function (n) { var p = n.parentElement; while (p) { if (!sel || matches(p, sel)) out.push(p); p = p.parentElement; } });
      return wrap(uniq(out));
    };
    proto.closest = function (sel) { return wrap(uniq(this.nodes.map(function (n) { return n.closest ? n.closest(sel) : null; }))); };
    proto.children = function (sel) {
      var out = [];
      this.nodes.forEach(function (n) { Array.prototype.forEach.call(n.children || [], function (c) { if (!sel || matches(c, sel)) out.push(c); }); });
      return wrap(out);
    };
    proto.contents = function () {
      var out = [];
      this.nodes.forEach(function (n) { Array.prototype.forEach.call(n.childNodes || [], function (c) { out.push(c); }); });
      return wrap(out);
    };
    proto.siblings = function (sel) {
      var out = [];
      this.nodes.forEach(function (n) {
        if (!n.parentElement) return;
        Array.prototype.forEach.call(n.parentElement.children, function (c) { if (c !== n && (!sel || matches(c, sel))) out.push(c); });
      });
      return wrap(uniq(out));
    };
    proto.next = function (sel) { return wrap(this.nodes.map(function (n) { return n.nextElementSibling; }).filter(function (n) { return n && (!sel || matches(n, sel)); })); };
    proto.prev = function (sel) { return wrap(this.nodes.map(function (n) { return n.previousElementSibling; }).filter(function (n) { return n && (!sel || matches(n, sel)); })); };
    proto.filter = function (f) {
      return wrap(this.nodes.filter(function (n, i) { return typeof f === 'function' ? f.call(n, i, n) : matches(n, f); }));
    };
    proto.not = function (sel) { return wrap(this.nodes.filter(function (n) { return !matches(n, sel); })); };
    proto.is = function (sel) { return this.nodes.some(function (n) { return matches(n, sel); }); };
    proto.hasClass = function (c) { return this.nodes.some(function (n) { return n.classList && n.classList.contains(c); }); };
    proto.remove = function () { this.nodes.forEach(function (n) { if (n.parentNode) n.parentNode.removeChild(n); }); return this; };

    var $ = function (sel, ctx) {
      if (sel == null) return wrap([]);
      if (typeof sel === 'string') {
        var s = sel.trim();
        if (s.charAt(0) === '<') {
          var frag = new DOMParser().parseFromString(s, 'text/html');
          return wrap(Array.prototype.slice.call(frag.body.children));
        }
        if (ctx) return (ctx.nodes ? ctx : $(ctx)).find(sel);
        return wrap(qsa(doc, sel));
      }
      if (sel.nodes) return sel;
      if (Array.isArray(sel)) return wrap(sel);
      return wrap([sel]);
    };
    $.html = function (w) { return w ? (w.nodes ? w.toString() : (w.outerHTML || '')) : doc.documentElement.outerHTML; };
    $.text = function () { return doc.body ? doc.body.textContent : ''; };
    $.root = function () { return wrap([doc.documentElement]); };
    return $;
  }
  var cheerio = { load: load, default: { load: load } };
  window.cheerio = cheerio;

  // ---------- Buffer (base64/utf8/hex subset) ----------
  function utf8ToB64(s) { return btoa(unescape(encodeURIComponent(s))); }
  function b64ToUtf8(s) { try { return decodeURIComponent(escape(atob(s))); } catch (e) { return atob(s); } }
  function JBuffer(str) { this._s = str; this.length = str.length; }
  JBuffer.prototype.toString = function (enc) {
    if (enc === 'base64') return utf8ToB64(this._s);
    if (enc === 'hex') return Array.prototype.map.call(unescape(encodeURIComponent(this._s)), function (c) { return ('0' + c.charCodeAt(0).toString(16)).slice(-2); }).join('');
    return this._s;
  };
  var Buffer = {
    from: function (input, enc) {
      if (enc === 'base64') return new JBuffer(b64ToUtf8(String(input)));
      if (enc === 'hex') { var s = ''; for (var i = 0; i < input.length; i += 2) s += String.fromCharCode(parseInt(input.substr(i, 2), 16)); return new JBuffer(s); }
      return new JBuffer(String(input));
    },
    isBuffer: function (b) { return b instanceof JBuffer; }
  };
  window.Buffer = Buffer;

  var processShim = { env: {}, platform: 'android', version: 'v18.0.0', nextTick: function (fn) { Promise.resolve().then(fn); } };
  window.process = processShim;
  window.global = window;

  function requireShim(name) {
    switch (name) {
      case 'axios': return window.axios;
      case 'cheerio':
      case 'cheerio-without-node-native':
      case 'react-native-cheerio': return cheerio;
      case 'crypto-js':
        if (window.CryptoJS) return window.CryptoJS;
        throw new Error('crypto-js is not available in this runtime');
      case 'buffer': return { Buffer: Buffer };
      default: throw new Error('Module not supported: ' + name);
    }
  }
  window.require = requireShim;

  ['log', 'info', 'warn', 'error', 'debug'].forEach(function (lvl) {
    console[lvl] = function () {
      try { NativeBridge.log(lvl + ': ' + Array.prototype.map.call(arguments, function (a) { return typeof a === 'string' ? a : JSON.stringify(a); }).join(' ')); } catch (e) {}
    };
  });

  window.__runScraper = function (callId, tmdbId, mediaType, season, episode) {
    try {
      var code = NativeBridge.getCode(callId);
      var module = { exports: {} };
      var fn = new Function('module', 'exports', 'require', code);
      fn(module, module.exports, requireShim);
      var ex = module.exports || {};
      var getStreams = ex.getStreams || (ex.default && ex.default.getStreams) || (typeof ex === 'function' ? ex : null);
      if (typeof getStreams !== 'function') throw new Error('getStreams() is not exported');
      Promise.resolve(getStreams(tmdbId, mediaType, season, episode)).then(function (r) {
        NativeBridge.result(callId, JSON.stringify(Array.isArray(r) ? r : (r && r.streams) || []));
      }).catch(function (e) {
        NativeBridge.error(callId, String((e && e.message) || e));
      });
    } catch (e) {
      NativeBridge.error(callId, String((e && e.message) || e));
    }
  };

  NativeBridge.ready();
})();
