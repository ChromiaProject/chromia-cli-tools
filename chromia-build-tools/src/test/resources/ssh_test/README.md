# SSH Keys

`ssh_test` / `ssh_test.pub` is a dedicated, **read-only deploy key** for a single
throwaway dummy repository used by `GitRepositoryClonerIT` in this test suite:

    git@gitlab.com:chromaway/core-tools/gitclonetestrepo.git

The private key is committed on purpose so the SSH clone integration tests can run
unattended in CI. This is safe only because the key:

- is a **read-only** GitLab deploy key (no push access),
- is scoped to that single dummy repo, which holds no real content,
- must **never** be added to a personal account or any other repository.

If the tests start failing with "repository not found" / `NoRemoteRepositoryException`,
the deploy key has lost access — re-add `ssh_test.pub` as a read-only deploy key on the
repo (Settings → Repository → Deploy keys), or rotate the keypair and update these files.

Read more: https://docs.gitlab.com/ee/user/project/deploy_keys/
