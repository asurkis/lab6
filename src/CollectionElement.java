import java.io.Serializable;

public class CollectionElement implements Serializable {
    private String someString;

    public CollectionElement(String someString) {
        this.someString = someString;
    }

    public CollectionElement() {
        this("");
    }

    public String getSomeString() {
        return someString;
    }

    public void setSomeString(String someString) {
        this.someString = someString;
    }
}
