package sk.upjs.ics.kopr;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class Runner {
    public static void main(String[] args) {
        var system = ActorSystem.create(WordFrequencyCounter.create(), "system");

        CompletionStage<WordFrequencyCounter.FrequenciesCalculated> result = AskPattern.ask(system,
                      replyTo -> new WordFrequencyCounter.CalculateFrequencies("dog dog dog", replyTo), Duration.ofSeconds(5),
                      system.scheduler()
                );

        result.thenAccept(event -> System.out.println(event));
    }
}
