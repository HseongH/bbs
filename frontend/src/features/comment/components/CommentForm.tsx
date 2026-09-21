import { useActionState } from "react";
import { fieldErrors, toProblem } from "@/api/problem";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";

type FormState = { error: string | null };

export function CommentForm({
  label,
  submitLabel,
  defaultValue,
  onSubmit,
}: {
  label: string;
  submitLabel: string;
  defaultValue?: string;
  onSubmit: (body: string) => Promise<void>;
}) {
  const [state, submit, isPending] = useActionState(
    async (_previous: FormState, formData: FormData): Promise<FormState> => {
      try {
        await onSubmit(String(formData.get("body") ?? ""));
        return { error: null };
      } catch (error) {
        const fields = fieldErrors(error);
        return { error: fields.body ?? toProblem(error)?.detail ?? "등록하지 못했습니다." };
      }
    },
    { error: null },
  );

  return (
    <form action={submit} className="space-y-1">
      <Textarea name="body" rows={3} defaultValue={defaultValue} aria-label={label} />
      {state.error ? <p className="text-sm text-red-600">{state.error}</p> : null}
      <Button type="submit" size="sm" disabled={isPending}>
        {submitLabel}
      </Button>
    </form>
  );
}
