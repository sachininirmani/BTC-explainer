import React, { useEffect, useMemo, useState } from "react";
import PriceChart, { ChartPoint, EventPoint } from "../components/PriceChart";
import { apiGet } from "../api/client";

type Factor = {
  name: string;
  score: number;
  evidence: Record<string, any>;
};

type Explanation = {
  eventId: number;
  eventDate: string;
  direction: string;
  pctChange: string;
  confidence: string;
  summary: string;
  aiExplanation?: string | null;
  aiSource?: string | null;
  aiModel?: string | null;
  factorsJson: string;
};

function buildFallbackNarrative(explain: Explanation, factors: Factor[]): string {
  const dirWord = explain.direction === "UP" ? "rose" : "fell";
  const pct = Math.abs(Number(explain.pctChange)).toFixed(2);

  const find = (name: string) => factors.find(f => f?.name === name);
  const news = find("News activity");
  const fng = find("Market sentiment (Fear & Greed)");
  const fx = find("FX context (EUR/USD)");

  const parts: string[] = [];
  parts.push(`Bitcoin ${dirWord} ${pct}% on ${explain.eventDate}.`);

  if (fng?.evidence?.classification && (fng.evidence?.value ?? null) !== null) {
    parts.push(
      `The Fear & Greed index sat in the “${String(fng.evidence.classification)}” range (value ${String(
        fng.evidence.value
      )}), which suggests a cautious backdrop.`
    );
  }

  const headlines = Array.isArray(news?.evidence?.sampleHeadlines)
    ? (news!.evidence.sampleHeadlines as any[]).map(String).slice(0, 6)
    : [];

  if (headlines.length > 0) {
    const h = headlines.join(" ").toLowerCase();
    const themes: string[] = [];
    if (h.includes("etf")) themes.push("ETF flows");
    if (h.includes("liquidat")) themes.push("liquidations and derivatives positioning");
    if (h.includes("analyst") || h.includes("bernstein") || h.includes("strategy")) themes.push("analyst/institutional commentary");
    if (h.includes("quantum") || h.includes("wallet")) themes.push("security topics");
    if (themes.length === 0) themes.push("general crypto market developments");
    parts.push(`News discussion around the date leaned toward ${themes.join(", ")}.`);
  } else {
    parts.push("News signals were limited for this exact date, so the move may have been driven more by technical trading and short-term positioning.");
  }

  if (fx?.evidence?.dayChangePct) {
    parts.push(`EUR/USD moved ${String(fx.evidence.dayChangePct)}% on the day, which can slightly influence broader risk sentiment.`);
  }

  parts.push(
    `Confidence is ${explain.confidence}, so treat this as an educational, correlation-based summary rather than advice.`
  );

  return parts.join(" ").replace(/\s+/g, " ").trim();
}

function scoreLabel(score: number): "HIGH" | "MED" | "LOW" {
  if (score >= 60) return "HIGH";
  if (score >= 30) return "MED";
  return "LOW";
}

export default function Dashboard() {
  const [chart, setChart] = useState<ChartPoint[]>([]);
  const [events, setEvents] = useState<EventPoint[]>([]);
  const [selected, setSelected] = useState<number | null>(null);
  const [explain, setExplain] = useState<Explanation | null>(null);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        setErr(null);
        const [c, e] = await Promise.all([
          apiGet<ChartPoint[]>(`/api/chart?days=180`),
          apiGet<EventPoint[]>(`/api/events?limit=100`)
        ]);
        setChart(c);
        setEvents(e);
      } catch (ex: any) {
        setErr(ex.message ?? "Failed to load");
      }
    })();
  }, []);

  async function selectEvent(id: number) {
    setSelected(id);
    setLoading(true);
    try {
      const ex = await apiGet<Explanation>(`/api/explain/${id}`);
      setExplain(ex);
    } catch (e: any) {
      setExplain(null);
      setErr(e.message ?? "Failed to explain");
    } finally {
      setLoading(false);
    }
  }

  const eventsSorted = useMemo(() => {
    return [...events].sort((a, b) => (a.date < b.date ? 1 : -1));
  }, [events]);

  const factors: Factor[] = useMemo(() => {
    if (!explain?.factorsJson) return [];
    try {
      const parsed = JSON.parse(explain.factorsJson);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }, [explain]);

  
return (
    <div className="container">
      <div className="topbar">
        <div className="brand">
          <div className="logo" aria-hidden="true" />
          <div>
            <h1>BTC Move Explainer</h1>
            <p>Eye-candy dashboard • educational, correlation-based summaries for large daily BTC moves</p>
          </div>
        </div>

        <div className="chipRow">
          <div className="chip">
            <strong>{events.length}</strong> events
          </div>
          <div className="chip">
            Chart: <strong>{chart.length ? `${chart.length} pts` : "—"}</strong>
          </div>
          <div className="chip">
            Backend: <strong>{import.meta.env.VITE_API_BASE_URL ? "env" : "localhost"}</strong>
          </div>
        </div>
      </div>

      {err && <div className="error">{err}</div>}

      <div className="grid">
        {/* Left: event list */}
        <div className="card">
          <div className="cardHeader">
            <div className="cardTitle">
              <h2>Market moves</h2>
              <span>Pick a day to see the signal breakdown and narrative</span>
            </div>
            <span className="badge">
              <strong>Tip</strong> Click a chart dot or an event
            </span>
          </div>
          <div className="cardBody">
            <div className="list">
              {eventsSorted.map((e) => {
                const pct = Number(e.pctChange);
                const cls = pct >= 0 ? "up" : "down";
                return (
                  <div
                    key={e.id}
                    className={`item ${selected === e.id ? "item-selected" : ""}`}
                    onClick={() => selectEvent(e.id)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(ev) => {
                      if (ev.key === "Enter" || ev.key === " ") selectEvent(e.id);
                    }}
                  >
                    <div className="itemLeft">
                      <div className="itemTop">
                        <span className={`badge badge-sev-${e.severity}`}>S{e.severity}</span>
                        <span className="badge">{e.direction}</span>
                        <span className="itemDate">{e.date}</span>
                      </div>
                      <div className="itemSub">Event id: {e.id}</div>
                    </div>

                    <div className="itemRight">
                      <div className={`itemPct ${cls}`}>{pct.toFixed(2)}%</div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Right: chart + explanation */}
        <div className="card">
          <div className="cardHeader">
            <div className="cardTitle">
              <h2>BTC close price</h2>
              <span>180-day view with clickable move markers</span>
            </div>

            {explain ? (
              <span className="badge">
                <strong>{explain.direction}</strong> • {explain.pctChange}% • conf {explain.confidence}
              </span>
            ) : (
              <span className="badge">
                <strong>Select</strong> an event to begin
              </span>
            )}
          </div>

          <div className="chartWrap">
            <div className="chartInner">
              <PriceChart data={chart} events={events} onSelectEvent={selectEvent} />
            </div>
            <div className="chartNote">
              <span className="muted">
                The explanation is correlation-based using stored signals and headlines (not financial advice).
              </span>
            </div>
          </div>

          <div className="cardBody">
            <div className="drawer">
              {loading && <div className="loading">Generating explanation…</div>}
              {!loading && !explain && (
                <div className="muted">Choose a move from the left list (or click a dot on the chart).</div>
              )}

              {explain && (
                <>
                  <div className="aiCard">
                    <div className="aiHead">
                      <div className="kv">
                        <div>
                          <span className="k">Date:</span> {explain.eventDate}
                        </div>
                        <div>
                          <span className="k">Move:</span> {explain.direction} {explain.pctChange}%
                        </div>
                        <div>
                          <span className="k">Confidence:</span> {explain.confidence}
                        </div>
                      </div>

                      <span className={`pill pill-${(explain.aiSource || "fallback").toLowerCase()}`}>
                        {(explain.aiSource || "FALLBACK").toUpperCase()}
                        {explain.aiModel ? ` • ${explain.aiModel}` : ""}
                      </span>
                    </div>

                    <div className="aiBody">
                      {explain.aiExplanation && String(explain.aiExplanation).trim().length > 0
                        ? explain.aiExplanation
                        : buildFallbackNarrative(explain, factors)}
                    </div>

                    <div className="aiFoot">
                       •This is an educational summary.
                    </div>
                  </div>

                  <details>
                    <summary>Original system summary (raw)</summary>
                    <div style={{ marginTop: 10, color: "rgba(231,246,236,0.92)", lineHeight: 1.6 }}>
                      {explain.summary}
                    </div>
                  </details>

                  <div>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 10 }}>
                      <strong>Signals</strong>
                      <span className="muted">Top drivers around the event date</span>
                    </div>
                    <hr className="sep" />

                    <div className="factors">
                      {factors.map((f, idx) => {
                        const score = Number(f.score);
                        const scoreCls = score >= 0.7 ? "high" : score >= 0.4 ? "med" : "low";
                        return (
                          <div key={`${f.name}-${idx}`} className="factor">
                            <div className="factorHeader">
                              <div className="factorName">{f.name}</div>
                              <div className={`factorScore ${scoreCls}`}>{Math.round(score * 100)}%</div>
                            </div>

                            {/* Keep the existing per-factor rendering logic */}
                            {(f.name === "News activity" || f.name === "News intensity") && (
                              <>
                                <div className="kv">
                                  {f.evidence?.value != null && (
                                    <div>
                                      <span className="k">Intensity:</span> {String(f.evidence.value)}
                                    </div>
                                  )}
                                  {f.evidence?.sourceDate && (
                                    <div>
                                      <span className="k">Source date:</span> {String(f.evidence.sourceDate)}
                                    </div>
                                  )}
                                  {f.evidence?.articlesCount != null && (
                                    <div>
                                      <span className="k">articlesCount:</span> {String(f.evidence.articlesCount)}
                                    </div>
                                  )}
                                  {f.evidence?.coveragePct != null && (
                                    <div>
                                      <span className="k">coveragePct:</span> {String(f.evidence.coveragePct)}
                                    </div>
                                  )}
                                  {f.evidence?.queryTag && (
                                    <div>
                                      <span className="k">queryTag:</span> {String(f.evidence.queryTag)}
                                    </div>
                                  )}
                                </div>

                                {f.evidence?.note && <div className="note">{String(f.evidence.note)}</div>}

                                {Array.isArray(f.evidence?.sampleHeadlines) && f.evidence.sampleHeadlines.length > 0 ? (
                                  <ul className="headlines">
                                    {f.evidence.sampleHeadlines.slice(0, 20).map((t: any, i: number) => (
                                      <li key={i}>{String(t)}</li>
                                    ))}
                                  </ul>
                                ) : (
                                  <div className="muted">No headlines stored for this date.</div>
                                )}
                              </>
                            )}


                            {f.name === "Fear & Greed Index" && (
                              <>
                                <div className="kv">
                                  {f.evidence?.value != null && (
                                    <div>
                                      <span className="k">FGI:</span> {String(f.evidence.value)}
                                    </div>
                                  )}
                                  {f.evidence?.classification && (
                                    <div>
                                      <span className="k">Class:</span> {String(f.evidence.classification)}
                                    </div>
                                  )}
                                  {f.evidence?.sourceDate && (
                                    <div>
                                      <span className="k">Source date:</span> {String(f.evidence.sourceDate)}
                                    </div>
                                  )}
                                </div>
                                {f.evidence?.explanation && <div className="note">{String(f.evidence.explanation)}</div>}
                              </>
                            )}

                            {f.name === "FX context (EUR/USD)" && (
                              <>
                                <div className="kv">
                                  {f.evidence?.eurUsdRate != null && (
                                    <div>
                                      <span className="k">EUR/USD:</span> {String(f.evidence.eurUsdRate)}
                                    </div>
                                  )}
                                  {f.evidence?.dayChangePct != null && (
                                    <div>
                                      <span className="k">Day %:</span> {String(f.evidence.dayChangePct)}
                                    </div>
                                  )}
                                  {f.evidence?.sourceDate && (
                                    <div>
                                      <span className="k">Source date:</span> {String(f.evidence.sourceDate)}
                                    </div>
                                  )}
                                </div>
                                {f.evidence?.note && <div className="note">{String(f.evidence.note)}</div>}
                              </>
                            )}

                            {f.name === "Weather risk" && (
                              <>
                                <div className="kv">
                                  {f.evidence?.condition && (
                                    <div>
                                      <span className="k">Condition:</span> {String(f.evidence.condition)}
                                    </div>
                                  )}
                                  {f.evidence?.tempC != null && (
                                    <div>
                                      <span className="k">Temp:</span> {String(f.evidence.tempC)}°C
                                    </div>
                                  )}
                                  {f.evidence?.sourceDate && (
                                    <div>
                                      <span className="k">Source date:</span> {String(f.evidence.sourceDate)}
                                    </div>
                                  )}
                                </div>
                                {f.evidence?.note && <div className="note">{String(f.evidence.note)}</div>}
                              </>
                            )}

                            {/* Generic fallback for any other factor types */}
                            {["News activity", "News intensity", "Fear & Greed Index", "FX context (EUR/USD)", "Weather risk"].includes(
                              f.name
                            ) ? null : (

                              <>
                                {f.evidence?.explanation && <div className="note">{String(f.evidence.explanation)}</div>}
                                {f.evidence?.note && <div className="note">{String(f.evidence.note)}</div>}
                                {f.evidence && Object.keys(f.evidence).length > 0 ? (
                                  <div className="kv">
                                    {Object.entries(f.evidence)
                                      .slice(0, 6)
                                      .map(([k, v]) => (
                                        <div key={k}>
                                          <span className="k">{k}:</span> {String(v)}
                                        </div>
                                      ))}
                                  </div>
                                ) : (
                                  <div className="muted">No evidence captured.</div>
                                )}
                              </>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );

}
