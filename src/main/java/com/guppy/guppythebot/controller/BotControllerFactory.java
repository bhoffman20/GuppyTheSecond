package com.guppy.guppythebot.controller;

import com.guppy.guppythebot.BotApplicationManager;
import com.guppy.guppythebot.BotGuildContext;

import net.dv8tion.jda.core.entities.Guild;

public interface BotControllerFactory<T extends BotController> {
  Class<T> getControllerClass();

  T create(BotApplicationManager manager, BotGuildContext state, Guild guild);
}
