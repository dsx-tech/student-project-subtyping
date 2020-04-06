package examples;
import ann.Subtype;

public class Test {
    @Subtype(Euro.class)
    private static long var3 = 100;
    @Subtype(Dollar.class)
    private static int var5 = 50;

    public Test() {

    }

    private static void testMethod1() {
        @Subtype(Euro.class)
        int var1 = 1000;
        @Subtype(Dollar.class)
        int var2 = 200;
        long var4 = var1 + var3;
        BankAccount bankAccount1 = new BankAccount(var4);
        BankAccount bankAccount2 = new BankAccount(var1);
        bankAccount1.transferFrom(bankAccount2, var1);
        testMethod2(var2 + testMethod2(var2));
    }

    @Subtype(Dollar.class)
    public static int testMethod2(@Subtype(Dollar.class) int sum) {
        @Subtype(Dollar.class)
        int value = sum;
        return value;
    }

    public static void main(String[] args) {
        testMethod1();
        testMethod2(var5);
    }
}

