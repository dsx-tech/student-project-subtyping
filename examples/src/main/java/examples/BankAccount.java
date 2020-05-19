package examples;
import annotation.Type;

public class BankAccount {
    @Type(Euro.class)
    public long moneyInEuros = 100;

    public BankAccount(@Type(Euro.class) long sum) {
        this.moneyInEuros = sum;
    }

    public void add(@Type(Euro.class) int sum) {
        this.moneyInEuros += sum;
    }
    public void debit(@Type(Euro.class) int sum) {
        this.moneyInEuros -= sum;
    }

    @Type(Euro.class)
    public long transferFrom(BankAccount bAcc, @Type(Euro.class) int sum) {
        bAcc.debit(sum);
        this.add(sum);
        return bAcc.moneyInEuros;
    }
}
