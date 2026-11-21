export default function QuantityStepper({ value, min = 1, max = 99, onChange, disabled = false }) {
  const decrease = () => onChange(Math.max(min, value - 1));
  const increase = () => onChange(Math.min(max, value + 1));

  return (
    <div className="stepper">
      <button type="button" onClick={decrease} disabled={disabled || value <= min} aria-label="Decrease quantity">
        −
      </button>
      <input
        type="number"
        value={value}
        min={min}
        max={max}
        disabled={disabled}
        onChange={(event) => {
          const parsed = Number(event.target.value);
          if (!Number.isNaN(parsed)) {
            onChange(Math.max(min, Math.min(max, parsed)));
          }
        }}
        aria-label="Quantity"
      />
      <button type="button" onClick={increase} disabled={disabled || value >= max} aria-label="Increase quantity">
        +
      </button>
    </div>
  );
}
