import { Button } from "@/components/ui/button";

export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <nav className="flex justify-center gap-1 py-4" aria-label="페이지">
      {Array.from({ length: totalPages }, (_, index) => index).map((index) => (
        <Button
          key={index}
          type="button"
          size="sm"
          variant={index === page ? "default" : "ghost"}
          aria-current={index === page ? "page" : undefined}
          onClick={() => onChange(index)}
        >
          {index + 1}
        </Button>
      ))}
    </nav>
  );
}
