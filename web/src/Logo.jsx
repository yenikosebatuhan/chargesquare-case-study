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
      <g fill="none" stroke={stroke} strokeWidth="31" strokeLinecap="round">
        <line x1="81" y1="17" x2="21" y2="73" />
        <line x1="18" y1="89" x2="118" y2="82" />
        <line x1="115" y1="78" x2="47" y2="164" />
      </g>
    </svg>
  );
}
