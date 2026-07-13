import { useId } from "react";

// ChargeSquare brand mark — the three-bar lightning bolt, traced from the company logo.
// Rendered as crisp SVG (no external asset) so it scales and themes cleanly.
export default function Logo({ size = 28, variant = "brand", style }) {
  const id = useId();
  const stroke = variant === "mono" ? "currentColor" : `url(#${id})`;
  return (
    <svg width={size} height={size * (181 / 136)} viewBox="0 0 136 181" style={style} aria-label="ChargeSquare">
      {variant === "brand" && (
        <defs>
          <linearGradient id={id} x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stopColor="#34d399" />
            <stop offset="1" stopColor="#16a34a" />
          </linearGradient>
        </defs>
      )}
      <g fill="none" stroke={stroke} strokeWidth="27" strokeLinecap="round">
        <line x1="80" y1="14" x2="20" y2="72" />
        <line x1="17" y1="89" x2="119" y2="81" />
        <line x1="116" y1="77" x2="46" y2="166" />
      </g>
    </svg>
  );
}
