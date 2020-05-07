package examples;
import ann.Type;
import ann.UnsafeCast;

public class Test {
    @Type(Euro.class)
    private static long var3 = 100;
    @Type(Dollar.class)
    private static int var5 = 50;

    public Test() {

    }

    private static void testMethod1() {
        @UnsafeCast
        @Type(SubEuro.class)
        int var1 = var5;

        @Type(Dollar.class)
        int var2 = 200;

        long var4 = var1 + var3;

        BankAccount bankAccount1 = new BankAccount(var4);
        BankAccount bankAccount2 = new BankAccount(var1);

        bankAccount1.transferFrom(bankAccount2, var1);
        testMethod2(var2 + testMethod2(var2));
    }

    @Type(Dollar.class)
    public static int testMethod2(@Type(Dollar.class) int sum) {
        int value = sum;
        @Type(Dollar.class)
        int value1 = 123;
        if (String.valueOf(value).equals("1")) {
            return value;
        } else {
            return value1;
        }
    }
    @Type(Dollar.class)
    public static int testMethod3() {
        return testMethod2(var5);
    }

    public static void main(String[] args) {
        testMethod1();
        testMethod2(var5);
    }
}

