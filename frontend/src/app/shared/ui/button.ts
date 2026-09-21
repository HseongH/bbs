import { ChangeDetectionStrategy, Component, computed, input } from "@angular/core";

type Variant = "default" | "outline" | "destructive" | "ghost";
type Size = "default" | "sm";

const VARIANT: Record<Variant, string> = {
  default: "bg-slate-900 text-white hover:bg-slate-800",
  outline: "border border-slate-300 hover:bg-slate-50",
  destructive: "border border-red-300 text-red-600 hover:bg-red-50",
  ghost: "text-slate-600 hover:bg-slate-100",
};

const SIZE: Record<Size, string> = {
  default: "px-4 py-2 text-sm",
  sm: "px-3 py-1 text-sm",
};

@Component({
  selector: "app-button",
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button [type]="type()" [disabled]="disabled()" [class]="classes()">
      <ng-content />
    </button>
  `,
})
export class ButtonComponent {
  readonly type = input<"button" | "submit">("button");
  readonly variant = input<Variant>("default");
  readonly size = input<Size>("default");
  readonly disabled = input(false);

  protected readonly classes = computed(
    () => `rounded disabled:opacity-50 ${VARIANT[this.variant()]} ${SIZE[this.size()]}`,
  );
}
