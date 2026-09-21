import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export function SearchForm({
  defaultKeyword,
  onSearch,
}: {
  defaultKeyword: string;
  onSearch: (keyword: string) => void;
}) {
  return (
    <form
      className="flex gap-2"
      onSubmit={(event) => {
        event.preventDefault();
        const formData = new FormData(event.currentTarget);
        onSearch(String(formData.get("keyword") ?? ""));
      }}
    >
      <Input
        name="keyword"
        defaultValue={defaultKeyword}
        placeholder="제목이나 본문으로 검색"
        aria-label="검색어"
        className="flex-1"
      />
      <Button type="submit">검색</Button>
    </form>
  );
}
