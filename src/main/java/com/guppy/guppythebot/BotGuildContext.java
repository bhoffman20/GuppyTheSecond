package com.guppy.guppythebot;

import java.util.HashMap;
import java.util.Map;

import com.guppy.guppythebot.controller.BotController;

public class BotGuildContext {
  public final long guildId;
  public final Map<Class<? extends BotController>, BotController> controllers;

  public BotGuildContext(long guildId) {
    this.guildId = guildId;
    this.controllers = new HashMap<>();
  }
}
