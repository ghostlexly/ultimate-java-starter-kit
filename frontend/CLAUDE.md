@AGENTS.md

## Conventions

- Never use optimistic updates in React Query mutations. Always wait for the server response before updating the cache.
- Never use nested ternaries. Extract them into early returns, if/else blocks, or dedicated functions.

## Styling

- Use Tailwind classes only. Do not add custom utility classes to `app/globals.css` — if a style is reused, build a
  component for it.
- Do not stack redundant `max-w-*` constraints between a parent and its child. A single source of truth for width.

## Page layout

- For any top-level page with wide content (catalog, checkout, account, etc.), wrap the content in `<PageContainer>`
  from `@/components/shared/page-container`. It handles centering, max width (1200px), and horizontal padding.
    - Pass vertical padding via `className` (e.g. `className="py-12"`), never add `max-w-*` on top.
    - Use the `as` prop when nesting inside another semantic element (`as="section"`, `as="div"`, etc.).
- Header and footer manage their own edge-to-edge backgrounds; they use plain `mx-auto max-w-[1200px]` inline and do not
  need `<PageContainer>`.

## Forms

- Always build forms with the shadcn `Field*` primitives from `@/components/ui/field` — never reinvent spacing with
  `space-y-*` or wrap inputs in plain `<div>`s.
- Standard structure:
  ```tsx
  <form onSubmit={(e) => {
    form.clearErrors();
    form.handleSubmit(handleSubmit)(e);
  }} noValidate>
    <FieldSet>
      <FieldLegend>…</FieldLegend>          {/* optional: group title */}
      <FieldDescription>…</FieldDescription> {/* optional: group context */}
      <FieldGroup>
        <Field data-invalid={fieldState.invalid}>
          <FieldLabel htmlFor={…}>…</FieldLabel>
          <Input /* or Textarea, Select, etc. */ />
          <FieldDescription>…</FieldDescription> {/* optional: inline help */}
          <FieldError errors={[fieldState.error]} />
        </Field>
        {/* …other <Field> siblings */}
      </FieldGroup>
    </FieldSet>
  </form>
  ```

More examples are available here : https://ui.shadcn.com/docs/components/radix/field

- Use `react-hook-form` with `<Controller>` for controlled inputs. Pipe the field state through
  `data-invalid={fieldState.invalid}` and `aria-invalid={fieldState.invalid}`. Don't add rules.
- Let the `Input` / `Textarea` default styles handle the invalid state. Do not re-declare
  `aria-invalid:border-destructive …` classes on every input.
- To attach an icon (or any leading/trailing element) to an input, always use `InputGroup` +
  `InputGroupInput` / `InputGroupTextarea` + `InputGroupAddon` from `@/components/ui/input-group`.
  Never build it manually with a `relative` wrapper and an `absolute`-positioned icon.
  ```tsx
  <InputGroup className="h-14">
    <InputGroupInput {...field} />
    <InputGroupAddon>
      <Mail className="text-muted-foreground h-4 w-4" />
    </InputGroupAddon>
  </InputGroup>
  ```
  For a textarea, use `align="inline-start"` on the addon (and `self-start pt-*` if the icon
  should stick to the top) so it doesn't vertically center on a multi-line field.

### Container / presenter pattern for forms backed by API data

Whenever a form's `defaultValues` come from an API call, **never** use `useEffect` + `form.reset(...)`
to backfill them once the data arrives. Split the page into a **container** (data fetching + render
gating) and a **presenter** (pure form, `useForm` initialised from props that are already populated).

- The container owns: route params, the data query, loading skeleton, "not found" empty state.
- The presenter receives the loaded entity as a prop and is only rendered once that entity exists.
  Because the entity is in scope at mount time, `useForm({ defaultValues: ... })` works on the
  first render — no async patching needed, no flash of empty inputs, no re-sync bugs.
- Mutations (`useUpdate…`) live in the presenter alongside the form they belong to.
- Apply this pattern to **every** edit page.

```tsx
// Container — fetches, gates rendering, never touches useForm.
export default function EditPage({params}: Readonly<PageProps>) {
    const {id} = use(params);
    const query = useThing(id);
    const thing = query.data?.find((item) => item.id === id);

    return (
        <div>
            {query.isLoading && <Skeleton/>}
            {!query.isLoading && !thing && <EmptyState/>}
            {thing && <FormContent thing={thing}/>}
        </div>
    );
}

// Presenter — pure form, defaults populated from props at mount.
function FormContent({thing}: Readonly<{ thing: Thing }>) {
    const form = useForm<FormShape>({defaultValues: {name: thing.name}});
    const updateMutation = useUpdateThing();
    // …
}
```