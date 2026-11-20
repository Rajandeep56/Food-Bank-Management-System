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

    private User requireCoordinator(AuthService authService) {
        return authService.requireRole("Coordinator");
    }

    public Report generateReport(AuthService authService, Report report, ReportPeriodType periodType,int year,int month) {
        User coordinator = requireCoordinator(authService);
        if (report == null) {
            throw new IllegalArgumentException("Report cannot be null");
        }

        report.specifyPeriod(periodType, year, month);
        report.initializeMetadata(coordinator.getName());
        report.generate();
        report.markGenerated();
        return report;
    }

    public void displayReport(AuthService authService, Report report) {
        
         requireCoordinator(authService);

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