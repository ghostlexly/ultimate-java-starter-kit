#!/bin/bash
# Generate RSA key pair for JWT signing

set -e

TMPDIR=$(mktemp -d)
PRIVATE_KEY="$TMPDIR/private.pem"
PUBLIC_KEY="$TMPDIR/public.pem"

openssl genrsa 2048 > "$PRIVATE_KEY" 2>/dev/null
openssl rsa -in "$PRIVATE_KEY" -pubout > "$PUBLIC_KEY" 2>/dev/null

echo ""
echo "Add these to your .env file:"
echo ""
echo "APP_JWT_PRIVATE_KEY=$(base64 < "$PRIVATE_KEY" | tr -d '\n')"
echo ""
echo "APP_JWT_PUBLIC_KEY=$(base64 < "$PUBLIC_KEY" | tr -d '\n')"
echo ""

rm -rf "$TMPDIR"
