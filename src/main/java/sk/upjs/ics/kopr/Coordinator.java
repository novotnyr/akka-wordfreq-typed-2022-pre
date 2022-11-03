package sk.upjs.ics.kopr;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Coordinator extends AbstractBehavior<Coordinator.Command> {
    private ActorRef<WordFrequencyCounter.Command> worker;

    private ActorRef<WordFrequencyCounter.FrequenciesCalculated> messageAdapter;

    private Coordinator(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(Coordinator::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CalculateFrequencies.class, this::calculateFrequencies)
                .build();
    }

    private Behavior<Command> calculateFrequencies(CalculateFrequencies command) {
        var sentence = command.sentence();
        this.worker.tell(new WordFrequencyCounter.CalculateFrequencies(sentence, messageAdapter));
        return Behaviors.same();
    }

    public interface Command {}

    public interface Event{}

    public record CalculateFrequencies(String sentence) implements Command{}
}
