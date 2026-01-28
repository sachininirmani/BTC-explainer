import React from "react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
  ReferenceDot,
  Brush
} from "recharts";

export type ChartPoint = {
  date: string;
  open: string;
  high: string;
  low: string;
  close: string;
};

export type EventPoint = {
  id: number;
  date: string;
  direction: string;
  pctChange: string;
  severity: number;
};

function severityColor(sev: number): string {
  switch (sev) {
    case 4:
      return "#b42318"; // deep red
    case 3:
      return "#d92d20"; // red
    case 2:
      return "#f59e0b"; // amber
    default:
      return "#22c55e"; // green
  }
}

export default function PriceChart({
  data,
  events,
  onSelectEvent
}: {
  data: ChartPoint[];
  events: EventPoint[];
  onSelectEvent: (id: number) => void;
}) {
  const series = data.map((p) => ({
    date: p.date,
    close: Number(p.close)
  }));

  const eventMap = new Map(events.map((e) => [e.date, e]));
  const dots = series
    .filter((p) => eventMap.has(p.date))
    .map((p) => ({ ...p, event: eventMap.get(p.date)! }));

  return (
    <div style={{ height: 420 }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={series}>
          <CartesianGrid strokeDasharray="3 3" opacity={0.25} />

          <XAxis
            dataKey="date"
            tick={{ fontSize: 11 }}
            minTickGap={24}
          />

          <YAxis
            tick={{ fontSize: 11 }}
            domain={["auto", "auto"]}
          />

          <Tooltip />

          <Line
            type="monotone"
            dataKey="close"
            dot={false}
            strokeWidth={2}
            stroke="#16a34a"
          />

          {dots.map((d) => (
            <ReferenceDot
              key={d.date}
              x={d.date}
              y={d.close}
              r={6}
              isFront={true}
              fill={severityColor(d.event.severity)}
              stroke="#ffffff"
              strokeWidth={2}
              onClick={() => onSelectEvent(d.event.id)}
            />
          ))}

          {/* 
            Brush enables horizontal scrolling / zooming
            This allows navigating older BTC price history
          */}
          <Brush
            dataKey="date"
            height={28}
            stroke="#16a34a"
            travellerWidth={10}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
