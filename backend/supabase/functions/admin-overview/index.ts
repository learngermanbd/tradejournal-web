import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "GET") return json({ error: "Method not allowed" }, 405);

  const authorization = request.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) return json({ error: "Authentication required" }, 401);

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL") ?? "",
    Deno.env.get("SUPABASE_ANON_KEY") ?? "",
    { global: { headers: { Authorization: authorization } } },
  );

  const { data: userData, error: userError } = await supabase.auth.getUser();
  if (userError || !userData.user) return json({ error: "Invalid session" }, 401);

  const { data: role, error: roleError } = await supabase
    .from("user_roles")
    .select("role")
    .eq("user_id", userData.user.id)
    .eq("role", "admin")
    .maybeSingle();
  if (roleError || !role) return json({ error: "Admin role required" }, 403);

  const [profiles, subscriptions, events] = await Promise.all([
    supabase.from("profiles").select("id", { count: "exact", head: true }),
    supabase.from("subscriptions").select("id", { count: "exact", head: true }).eq("status", "active"),
    supabase.from("service_events").select("service_name,status,created_at").order("created_at", { ascending: false }).limit(20),
  ]);

  const failed = [profiles.error, subscriptions.error, events.error].find(Boolean);
  if (failed) return json({ error: "Unable to load admin metrics" }, 500);

  return json({
    generatedAt: new Date().toISOString(),
    activeUsers: profiles.count ?? 0,
    activeSubscriptions: subscriptions.count ?? 0,
    recentServiceEvents: events.data ?? [],
    privacy: {
      rawTradesIncluded: false,
      journalNotesIncluded: false,
      psychologyIncluded: false,
    },
  });
});
