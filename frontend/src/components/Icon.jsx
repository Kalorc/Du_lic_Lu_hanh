const shapes = {
  compass: <><circle cx="12" cy="12" r="9" /><path d="m15.5 8.5-2 5-5 2 2-5 5-2Z" /></>,
  pin: <><path d="M19 10c0 5.2-7 10-7 10S5 15.2 5 10a7 7 0 1 1 14 0Z" /><circle cx="12" cy="10" r="2.5" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  search: <><circle cx="10.5" cy="10.5" r="6" /><path d="m15 15 4.5 4.5" /></>,
  sliders: <><path d="M4 6h7m4 0h5M4 12h3m4 0h9M4 18h10m4 0h2" /><circle cx="13" cy="6" r="2" /><circle cx="9" cy="12" r="2" /><circle cx="16" cy="18" r="2" /></>,
  forward: <path d="M4 12h15m-6-6 6 6-6 6" />,
  back: <path d="M20 12H5m6-6-6 6 6 6" />,
  down: <path d="M12 4v15m-6-6 6 6 6-6" />,
  landscape: <><rect x="3" y="4" width="18" height="16" rx="2" /><circle cx="15.5" cy="8.5" r="1.5" /><path d="m3 17 5-5 4 4 2-2 7 5" /></>,
  lock: <><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V7a4 4 0 1 1 8 0v3m-4 4v3" /></>,
  check: <path d="m5 12 4 4L19 6" />,
};

export default function Icon({ name, className }) {
  return (
    <svg
      className={className}
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.65"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {shapes[name]}
    </svg>
  );
}
