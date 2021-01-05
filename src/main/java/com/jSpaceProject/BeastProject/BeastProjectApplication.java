package com.jSpaceProject.BeastProject;

import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BeastProjectApplication {

    public static void main(String[] args) throws InterruptedException {

        Space inbox = new SequentialSpace();

        inbox.put("Hello World!");
        Object[] tuple = inbox.get(new FormalField(String.class));
        System.out.println(tuple[0]);

        SpringApplication.run(BeastProjectApplication.class, args);

    }
}


