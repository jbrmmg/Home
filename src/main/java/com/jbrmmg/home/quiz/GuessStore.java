package com.jbrmmg.home.quiz;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class GuessStore {
    private final List<Guess> guesses = new ArrayList<>();

    public void add(Guess guess) {
        guesses.add(guess);
    }

    public List<Guess> getAll() {
        return Collections.unmodifiableList(guesses);
    }

    public void reset() {
        guesses.clear();
    }
}
