package com.btbaazlaunda.lull.shizuku;

/** Runs inside Shizuku's shell-privileged process. */
interface IShellService {
    /** Reserved by Shizuku: called when the service is unbound with remove = true. */
    void destroy() = 16777114;

    /** Moves the named feature to its night or day state; true when the command succeeded. */
    boolean apply(String feature, boolean asleep) = 1;
}
