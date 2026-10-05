import java.util.*;

public class Poker {
    static Hand hand(Object... args) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < args.length; i += 2) {
            cards.add(new Card((Rank) args[i], (Suit) args[i + 1]));
        }
        return new Hand(cards);
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Hand wheel = hand(Rank.ACE, Suit.CLUBS, Rank.TWO, Suit.HEARTS, Rank.THREE, Suit.SPADES,
                Rank.FOUR, Suit.CLUBS, Rank.FIVE, Suit.HEARTS);
        Hand broadway = hand(Rank.TEN, Suit.CLUBS, Rank.JACK, Suit.HEARTS, Rank.QUEEN, Suit.SPADES,
                Rank.KING, Suit.CLUBS, Rank.ACE, Suit.HEARTS);
        Hand trips = hand(Rank.NINE, Suit.CLUBS, Rank.NINE, Suit.HEARTS, Rank.NINE, Suit.SPADES,
                Rank.TWO, Suit.CLUBS, Rank.FIVE, Suit.HEARTS);
        Hand fullHouse = hand(Rank.KING, Suit.CLUBS, Rank.KING, Suit.HEARTS, Rank.KING, Suit.SPADES,
                Rank.TWO, Suit.CLUBS, Rank.TWO, Suit.HEARTS);
        Hand sevenCards = hand(Rank.ACE, Suit.SPADES, Rank.ACE, Suit.HEARTS, Rank.ACE, Suit.CLUBS,
                Rank.KING, Suit.SPADES, Rank.KING, Suit.HEARTS, Rank.TWO, Suit.CLUBS, Rank.THREE, Suit.CLUBS);

        check(wheel.handType() == HandType.STRAIGHT, "wheel phai la straight");
        check(broadway.compareTo(wheel) > 0, "straight cao nhat phai thang straight A-5");
        check(wheel.compareTo(trips) > 0, "straight phai thang three of a kind");
        check(fullHouse.compareTo(wheel) > 0, "full house phai thang straight");
        check(sevenCards.handType() == HandType.FULL_HOUSE, "7 la: tap con tot nhat la full house");

        System.out.println("Tat ca test deu dung!");
        System.out.println("wheel     -> " + wheel.handType());
        System.out.println("broadway  -> " + broadway.handType());
        System.out.println("trips     -> " + trips.handType());
        System.out.println("fullHouse -> " + fullHouse.handType());
        System.out.println("7 la      -> " + sevenCards.handType());

        Hand flush = hand(Rank.TWO, Suit.HEARTS, Rank.FIVE, Suit.HEARTS, Rank.NINE, Suit.HEARTS,
                Rank.JACK, Suit.HEARTS, Rank.KING, Suit.HEARTS);
        System.out.println("flush     -> " + flush.handType());
        System.out.println("flush > fullHouse? " + (flush.compareTo(fullHouse) > 0));
    }
}

enum Suit { CLUBS, DIAMONDS, HEARTS, SPADES }

enum Rank {
    ACE(1), TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7),
    EIGHT(8), NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13);

    private final int value;

    Rank(int value) { this.value = value; }

    /** Gia tri khi Ace tinh la quan cao nhat (dung de so sanh). */
    int highValue() { return this == ACE ? 14 : value; }
}

record Card(Rank rank, Suit suit) {
    @Override
    public String toString() { return rank + " of " + suit; }
}

enum HandType {
    HIGH_CARD, PAIR, TWO_PAIR, THREE_OF_A_KIND, STRAIGHT,
    FLUSH, FULL_HOUSE, FOUR_OF_A_KIND, STRAIGHT_FLUSH  // royal flush = straight flush cao nhat
}

/** Suc manh cua mot tay bai: (loai tay bai, danh sach gia tri de pha hoa). */
record Strength(HandType type, List<Integer> order) implements Comparable<Strength> {
    @Override
    public int compareTo(Strength other) {
        int c = type.compareTo(other.type);
        if (c != 0) return c;
        int n = Math.min(order.size(), other.order.size());
        for (int i = 0; i < n; i++) {
            c = Integer.compare(order.get(i), other.order.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(order.size(), other.order.size());
    }
}

class Hand implements Comparable<Hand> {
    private final List<Card> cards;

    Hand(List<Card> cards) { this.cards = new ArrayList<>(cards); }

    Strength strength() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Khong the danh gia tay bai rong");
        }
        if (cards.size() <= 5) {
            return evaluate(cards);
        }
        // Hon 5 la (vi du Texas hold'em): lay tap con 5 la tot nhat.
        Strength best = null;
        for (List<Card> subset : combinations(cards, 5)) {
            Strength s = evaluate(subset);
            if (best == null || s.compareTo(best) > 0) best = s;
        }
        return best;
    }

    HandType handType() { return strength().type(); }

    private static Strength evaluate(List<Card> cards) {
        // Dem so lan xuat hien cua moi gia tri
        Map<Integer, Integer> counts = new HashMap<>();
        for (Card c : cards) counts.merge(c.rank().highValue(), 1, Integer::sum);

        // Nhom theo (so lan, gia tri), giam dan
        List<Map.Entry<Integer, Integer>> groups = new ArrayList<>(counts.entrySet());
        groups.sort((a, b) -> a.getValue().equals(b.getValue())
                ? b.getKey() - a.getKey()
                : b.getValue() - a.getValue());

        List<Integer> order = new ArrayList<>();
        List<Integer> shape = new ArrayList<>();
        for (Map.Entry<Integer, Integer> g : groups) {
            order.add(g.getKey());
            shape.add(g.getValue());
        }

        boolean full = cards.size() == 5;
        Set<Suit> suits = new HashSet<>();
        for (Card c : cards) suits.add(c.suit());

        boolean flush = full && suits.size() == 1;
        boolean wheel = full && counts.keySet().equals(Set.of(14, 2, 3, 4, 5)); // A-2-3-4-5
        boolean straight = full && counts.size() == 5
                && (Collections.max(counts.keySet()) - Collections.min(counts.keySet()) == 4 || wheel);
        if (wheel) order = new ArrayList<>(List.of(5, 4, 3, 2, 1)); // ace-low: la 5 la cao nhat

        HandType type;
        if (straight && flush)                                   type = HandType.STRAIGHT_FLUSH;
        else if (shape.get(0) == 4)                              type = HandType.FOUR_OF_A_KIND;
        else if (shape.size() >= 2 && shape.get(0) == 3 && shape.get(1) == 2) type = HandType.FULL_HOUSE;
        else if (flush)                                          type = HandType.FLUSH;
        else if (straight)                                       type = HandType.STRAIGHT;
        else if (shape.get(0) == 3)                              type = HandType.THREE_OF_A_KIND;
        else if (shape.size() >= 2 && shape.get(0) == 2 && shape.get(1) == 2) type = HandType.TWO_PAIR;
        else if (shape.get(0) == 2)                              type = HandType.PAIR;
        else                                                     type = HandType.HIGH_CARD;

        return new Strength(type, order);
    }

    /** Sinh tat ca to hop k phan tu cua list. */
    private static List<List<Card>> combinations(List<Card> items, int k) {
        List<List<Card>> result = new ArrayList<>();
        build(items, k, 0, new ArrayList<>(), result);
        return result;
    }

    private static void build(List<Card> items, int k, int start,
                              List<Card> current, List<List<Card>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < items.size(); i++) {
            current.add(items.get(i));
            build(items, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    @Override
    public int compareTo(Hand other) { return strength().compareTo(other.strength()); }

    @Override
    public boolean equals(Object o) {
        return o instanceof Hand h && compareTo(h) == 0;
    }

    @Override
    public int hashCode() { return strength().hashCode(); }

    @Override
    public String toString() { return cards.toString(); }
}