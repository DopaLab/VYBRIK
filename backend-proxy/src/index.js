const ROUTES = {
  "/football/fixtures": "https://v3.football.api-sports.io/fixtures",
  "/mma/fights": "https://v1.mma.api-sports.io/fights"
};

const ALLOWED_QUERY = new Set(["league", "season", "from", "to", "timezone"]);

export default {
  async fetch(request, env, ctx) {
    if (request.method !== "GET") return new Response("Method not allowed", { status: 405 });
    if (!env.API_SPORTS_KEY) return new Response("Proxy is not configured", { status: 503 });

    const incoming = new URL(request.url);
    const upstreamBase = ROUTES[incoming.pathname];
    if (!upstreamBase) return new Response("Not found", { status: 404 });

    const upstream = new URL(upstreamBase);
    for (const [key, value] of incoming.searchParams) {
      if (ALLOWED_QUERY.has(key)) upstream.searchParams.set(key, value.slice(0, 64));
    }

    const cache = caches.default;
    const cacheKey = new Request(upstream.toString(), { method: "GET" });
    const cached = await cache.match(cacheKey);
    if (cached) return cached;

    const response = await fetch(upstream, {
      headers: { "x-apisports-key": env.API_SPORTS_KEY, "accept": "application/json" }
    });
    if (!response.ok) return new Response(response.body, { status: response.status });

    const safe = new Response(response.body, response);
    safe.headers.set("Cache-Control", "public, max-age=43200, s-maxage=43200");
    safe.headers.delete("set-cookie");
    ctx.waitUntil(cache.put(cacheKey, safe.clone()));
    return safe;
  }
};
