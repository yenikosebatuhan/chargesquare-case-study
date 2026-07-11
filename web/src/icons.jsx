// Tiny inline SVG icon set — no external icon fonts or CDNs (keeps the panel self-contained).
const base = { fill: "none", stroke: "currentColor", strokeWidth: 2, strokeLinecap: "round", strokeLinejoin: "round" };

export const Bolt = (p) => (
  <svg viewBox="0 0 24 24" {...base} {...p}><path d="M13 2 4 14h7l-1 8 9-12h-7l1-8Z" /></svg>
);
export const Plug = (p) => (
  <svg viewBox="0 0 24 24" {...base} {...p}><path d="M9 2v4M15 2v4M7 6h10v4a5 5 0 0 1-10 0V6ZM12 15v5" /></svg>
);
export const Activity = (p) => (
  <svg viewBox="0 0 24 24" {...base} {...p}><path d="M22 12h-4l-3 9L9 3l-3 9H2" /></svg>
);
export const Wallet = (p) => (
  <svg viewBox="0 0 24 24" {...base} {...p}><path d="M3 7a2 2 0 0 1 2-2h13v4M3 7v10a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-3M3 7h16M17 13h.01M21 10v4h-4a2 2 0 0 1 0-4h4Z" /></svg>
);
export const Stop = (p) => (
  <svg viewBox="0 0 24 24" {...base} {...p}><rect x="6" y="6" width="12" height="12" rx="2" /></svg>
);
export const Plus = (p) => (
  <svg viewBox="0 0 24 24" {...base} {...p}><path d="M12 5v14M5 12h14" /></svg>
);
export const Logout = (p) => (
  <svg viewBox="0 0 24 24" {...base} {...p}><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" /></svg>
);
export const Close = (p) => (
  <svg viewBox="0 0 24 24" {...base} {...p}><path d="M18 6 6 18M6 6l12 12" /></svg>
);
export const Warning = (p) => (
  <svg viewBox="0 0 24 24" {...base} {...p}><path d="M12 9v4M12 17h.01M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" /></svg>
);
export const Receipt = (p) => (
  <svg viewBox="0 0 24 24" {...base} {...p}><path d="M6 2h12v20l-3-2-3 2-3-2-3 2V2ZM9 7h6M9 11h6M9 15h4" /></svg>
);
export const Layers = (p) => (
  <svg viewBox="0 0 24 24" {...base} {...p}><path d="m12 2 9 5-9 5-9-5 9-5ZM3 12l9 5 9-5M3 17l9 5 9-5" /></svg>
);
