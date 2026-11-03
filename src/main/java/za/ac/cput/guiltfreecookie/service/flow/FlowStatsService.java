package za.ac.cput.guiltfreecookie.service.flow;

import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.EmailFlow;
import za.ac.cput.guiltfreecookie.dto.flow.FlowStats;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.repository.EmailSendRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class FlowStatsService {

    private final EmailSendRepository sendRepository;

    public FlowStatsService(EmailSendRepository sendRepository) {
        this.sendRepository = sendRepository;
    }

    // Every flow, in order, with zeros where nothing was sent.
    public List<FlowStats> since(int days) {
        if (days < 1 || days > 365) {
            throw new BadRequestException("days must be between 1 and 365");
        }
        Map<EmailFlow, FlowStats> byFlow = new EnumMap<>(EmailFlow.class);
        for (Object[] row : sendRepository.statsSince(LocalDateTime.now().minusDays(days))) {
            EmailFlow flow = (EmailFlow) row[0];
            byFlow.put(flow, new FlowStats(flow, (Long) row[1], (Long) row[2], (Long) row[3]));
        }
        return Arrays.stream(EmailFlow.values())
                .map(flow -> byFlow.getOrDefault(flow, new FlowStats(flow, 0, 0, 0)))
                .toList();
    }
}
