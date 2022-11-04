package sk.upjs.ics.kopr;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.PreRestart;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.util.Map;

public class WordFrequencyCounter extends AbstractBehavior<WordFrequencyCounter.Command> {
    private WordFrequencyCounter(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new WordFrequencyCounter(context));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CalculateFrequencies.class, this::calculateFrequencies)
                .onSignal(PreRestart.class, signal -> {
                    getContext().getLog().debug("Actor sa ide restartnut");
                    return Behaviors.same();
                })
                .onSignal(PostStop.class, signal -> {
                    getContext().getLog().debug("Actor sa stopol");
                    return Behaviors.same();
                })
                .build();
    }

    private Behavior<Command> calculateFrequencies(CalculateFrequencies command) {
        if(Math.random() < 0.5) {
            throw new IllegalStateException("Je mi zle");
        }

        if(Math.random() < 0.5) {
            throw new UnsupportedOperationException("Aktor nedokaze spracovat spravu: " + getContext().getSelf());
        }

        var sentence = command.sentence();

        var result = Stream.of(sentence.split("\\s"))
                           .collect(Collectors.groupingBy(String::toString, Collectors.counting()));

        getContext().getLog().debug("{} Frequencies: {}", getContext().getSelf(), result);

        command.replyTo().tell(new FrequenciesCalculated(result));

        return Behaviors.same();
    }

    public interface Command {}

    public interface Event {}

    public record CalculateFrequencies(String sentence, ActorRef<FrequenciesCalculated> replyTo) implements Command {}

    public record FrequenciesCalculated(Map<String, Long> frequencies) implements Event {}
}
