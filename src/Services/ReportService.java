package Services;

import Repository.UserRepository;
import model.User;
import model.Coordinator;
import model.Report;
import common.ReportPeriodType;

public class ReportService {

    private final UserRepository userRepository;

    public ReportService(UserRepository userRepository) {
        if (userRepository == null) {
            throw new IllegalArgumentException(
                "User repository cannot be null"
            );
        }

        this.userRepository = userRepository;
    }

    private Coordinator requireCoordinator(int coordinatorId) {

        if (coordinatorId <= 0) {
            throw new IllegalArgumentException(
                "Coordinator ID must be positive"
            );
        }

        User user = userRepository.findById(coordinatorId);

        if (!(user instanceof Coordinator)) {
            throw new SecurityException(
                "Only coordinators can generate reports"
            );
        }

        return (Coordinator) user;
    }

    public Report generateReport(int coordinatorId, Report report, ReportPeriodType periodType,int year,int month) {
        Coordinator coordinator = requireCoordinator(coordinatorId);
        if (report == null) {
            throw new IllegalArgumentException("Report cannot be null");
        }

        report.specifyPeriod(periodType, year, month);
        report.initializeMetadata(coordinator.getName());
        report.generate();
        report.markGenerated();
        return report;
    }

    public void displayReport(int coordinatorId, Report report) {
        
        requireCoordinator(coordinatorId);

        if (report == null) {
            throw new IllegalArgumentException(
                "Report cannot be null"
            );
        }
        if (!report.isGenerated()) {
            throw new IllegalStateException(
                "Report has not been successfully generated"
            );
        }
        if (report.getStartDate() == null ||
            report.getEndDate() == null) {

            throw new IllegalStateException(
                "Report period has not been specified"
            );
        }

        report.display();
    }
}