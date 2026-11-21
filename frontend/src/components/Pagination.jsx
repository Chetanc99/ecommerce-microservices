export default function Pagination({ page = 0, totalPages = 0, onChange }) {
  if (totalPages <= 1) {
    return null;
  }

  const pages = Array.from({ length: totalPages }, (_, index) => index).filter(
    (index) => index === 0 || index === totalPages - 1 || Math.abs(index - page) <= 1
  );

  return (
    <nav className="pagination" aria-label="Pagination">
      <button type="button" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        ← Previous
      </button>

      {pages.map((index, position) => {
        const previous = pages[position - 1];
        const gap = previous !== undefined && index - previous > 1;

        return (
          <span key={index} className="pagination__item">
            {gap && <span className="pagination__gap">…</span>}
            <button
              type="button"
              className={index === page ? 'is-active' : ''}
              aria-current={index === page ? 'page' : undefined}
              onClick={() => onChange(index)}
            >
              {index + 1}
            </button>
          </span>
        );
      })}

      <button type="button" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Next →
      </button>
    </nav>
  );
}
