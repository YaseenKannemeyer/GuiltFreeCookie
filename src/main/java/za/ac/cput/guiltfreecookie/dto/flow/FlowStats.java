package za.ac.cput.guiltfreecookie.dto.flow;

import za.ac.cput.guiltfreecookie.domain.EmailFlow;

// Opens are approximate: Apple Mail and some other apps load images without the person opening the email.
public record FlowStats(EmailFlow flow, long sent, long opened, long clicked) {
}
