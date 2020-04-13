package examples;
import ann.Subtype;

public class BankAccount {
    @Subtype(Euro.class)
    public long moneyInEuros = 100;

    public BankAccount(@Subtype(Euro.class) long sum) {
        this.moneyInEuros = sum;
    }

    public void add(@Subtype(Euro.class) int sum) {
        this.moneyInEuros += sum;
    }
    public void debit(@Subtype(Euro.class) int sum) {
        this.moneyInEuros -= sum;
    }

    @Subtype(Euro.class)
    public long transferFrom(BankAccount bAcc, @Subtype(Euro.class) int sum) {
        bAcc.debit(sum);
        this.add(sum);
        return bAcc.moneyInEuros;
    }
}
