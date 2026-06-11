### Interface

```ts
export interface SendSupportMessageRequest {
    email: string;
    message: string;
}
```

### Usage

The mutation is declared with `useMutation` directly in the component, in the same file as `handleSubmit`. Do not
extract it into a separate custom hook.

Backend validation errors are applied to the form imperatively inside `onError`: when `hasFieldErrors(error)` is true,
`handleApiErrors(form, error)` registers each violation via `form.setError` (and focuses the first one); otherwise we
fall back to a generic toast. No reactive hook is needed — the `form.clearErrors()` in the `onSubmit` wrapper wipes
stale server errors before each submit.

```tsx
import {useMutation} from '@tanstack/react-query';
import {toast} from 'sonner';
import {SendSupportMessageRequest} from '@/types/support';
import {wolfios} from '@/lib/wolfios';
import {getErrorMessage, handleApiErrors, hasFieldErrors} from '@/lib/errors';

const form = useForm<SupportFormValues>({
    defaultValues: {email: '', message: ''},
});

const sendSupportMessage = useMutation({
    mutationFn: (data: SendSupportMessageRequest) =>
        wolfios.post('/api/support', data).then((res) => res.data),
});

async function handleSubmit(values: SupportFormValues) {
    // The mutation will still throw an error if it fails, even with the onError
    await sendSupportMessage.mutateAsync(values, {
        onSuccess: async () => {
            toast.success(
                'Nous avons bien reçu votre message. Nous vous répondrons dans les plus brefs délais.',
            );
            form.reset();
        },
        onError: (error) => {
            // Field-level violations are applied inline on each Field — only toast for generic errors
            if (hasFieldErrors(error)) {
                handleApiErrors(form, error);
                return;
            }

            toast.error(getErrorMessage(error, "Erreur lors de l'envoi du message"));
        },
    });
}
```

The `onSubmit` wrapper clears previous server errors before delegating to `handleSubmit`:

```tsx
<form
    onSubmit={(event) => {
        form.clearErrors();
        form.handleSubmit(handleSubmit)(event);
    }}
    noValidate
>
    {/* …fields… */}

    <Button type="submit" loading={form.formState.isSubmitting}>
        Enregistrer
    </Button>
</form>
```
