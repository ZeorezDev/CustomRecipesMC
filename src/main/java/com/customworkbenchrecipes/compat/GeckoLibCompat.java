package com.customworkbenchrecipes.compat;

public final class GeckoLibCompat {
  private GeckoLibCompat() {}

  public static boolean isGeckoLibPresent() {
    try {
      Class.forName("software.bernie.geckolib.GeckoLib");
      return true;
    } catch (Throwable ignore) {
    }
    try {
      Class.forName("software.bernie.geckolib3.GeckoLib");
      return true;
    } catch (Throwable ignore) {
    }
    return false;
  }

  public static void ifPresent(Runnable action) {
    if (isGeckoLibPresent()) {
      try {
        action.run();
      } catch (Throwable t) {
        // Silently ignore errors in compatibility shim
      }
    }
  }
}
