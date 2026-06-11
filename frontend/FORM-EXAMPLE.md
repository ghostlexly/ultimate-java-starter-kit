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

```tsx
import {useMutation} from '@tanstack/react-query';
import {SendSupportMessageRequest} from '@/types/support';
import {wolfios} from '@/lib/wolfios';

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
            // Field-level errors are shown inline by EmailStep — only toast for generic errors
            if (hasFieldErrors(error)) {
                return;
            }


            toast.error(getErrorMessage(error, "Erreur lors de l'envoi du code"));
        },
    });
}

useBackendFormErrors(form, sendSupportMessage.error);

<Button type="submit" loading={form.formState.isSubmitting}>
    Enregistrer
</Button>
```
