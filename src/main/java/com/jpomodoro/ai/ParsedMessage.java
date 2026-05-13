package com.jpomodoro.ai;

import java.time.Instant;

public record ParsedMessage(String role, String content, Instant timestamp) {}
