# Security

## Reporting a vulnerability

Please report security issues privately to the repository owner through GitHub rather than opening a public issue containing exploit details, credentials, or personal data.

Include the affected VYBRIK version, Android version, reproduction steps, and the impact you observed.

## Secrets and signing

This repository intentionally excludes:

- `local.properties`
- API keys and Cloudflare development secrets
- signing keystores and passwords
- generated release artifacts

Never commit those files to a fork. APKs published in GitHub Releases should be verified against the checksum in `RELEASE_NOTES.md`.
