package tech.HTECH;

import org.bytedeco.opencv.opencv_objdetect.HOGDescriptor;
import java.lang.reflect.Method;

public class CheckHOG {
    public static void main(String[] args) {
        System.out.println("Checking HOGDescriptor methods...");
        try {
            for (Method m : HOGDescriptor.class.getMethods()) {
                if (m.getName().equals("compute")) {
                    System.out.println(m);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
