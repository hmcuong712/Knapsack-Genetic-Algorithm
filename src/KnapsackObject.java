public class KnapsackObject {
    public final double cost;
    public final double value;
    public int id;

    public KnapsackObject(int seed) {
        this.cost = Math.pow(seed, Math.random());
        this.value = Math.pow(seed, Math.random()) + Math.random();
    }

    public String toString() {
        return "[ID: " + id + ", cost: " + cost + ", value: " + value + "]";
    }
}
