package com.kickstarter.viewmodels;

import android.content.Intent;
import android.support.annotation.NonNull;

import com.kickstarter.KSRobolectricTestCase;
import com.kickstarter.factories.ProjectFactory;
import com.kickstarter.factories.UserFactory;
import com.kickstarter.libs.CurrentUserType;
import com.kickstarter.libs.Environment;
import com.kickstarter.libs.MockCurrentUser;
import com.kickstarter.models.Project;
import com.kickstarter.services.ApiClientType;
import com.kickstarter.services.MockApiClient;
import com.kickstarter.ui.IntentKey;

import org.junit.Test;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.observers.TestSubscriber;

public class ProjectViewModelTest extends KSRobolectricTestCase {

  @Test
  public void testProjectViewModel_EmitsProjectWithStandardSetUp() {
    final ProjectViewModel vm = new ProjectViewModel(environment());

    final TestSubscriber<Project> projectTest = new TestSubscriber<>();
    vm.outputs.projectAndConfig().map(pc -> pc.first)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeOn(AndroidSchedulers.mainThread())
      .subscribe(projectTest);

    final Project project = ProjectFactory.halfWayProject();
    vm.intent(new Intent().putExtra(IntentKey.PROJECT, project));

    projectTest.assertValues(project, project);

    koalaTest.assertValues("Project Page");
  }

  @Test
  public void testProjectViewModel_LoggedOutStarProjectFlow() {
    final CurrentUserType currentUser = new MockCurrentUser();

    final Environment environment = environment().toBuilder()
      .currentUser(currentUser)
      .build();

    final ProjectViewModel vm = new ProjectViewModel(environment);

    final TestSubscriber<Void> loginToutTest = new TestSubscriber<>();
    vm.outputs.showLoginTout().subscribe(loginToutTest);

    final TestSubscriber<Void> showStarredPromptTest = new TestSubscriber<>();
    vm.outputs.showStarredPrompt().subscribe(showStarredPromptTest);

    final TestSubscriber<Boolean> starredTest = new TestSubscriber<>();
    vm.outputs.projectAndConfig().map(pc -> pc.first).map(Project::isStarred).subscribe(starredTest);

    // Start the view model with a project
    vm.intent(new Intent().putExtra(IntentKey.PROJECT, ProjectFactory.halfWayProject()));

    // A koala event should NOT be tracked
    koalaTest.assertValues("Project Page");

    starredTest.assertValues(false, false);

    // Try starring while logged out
    vm.inputs.starClicked();

    // The project shouldn't be starred, and a login prompt should be shown.
    starredTest.assertValues(false, false);
    showStarredPromptTest.assertValueCount(0);
    loginToutTest.assertValueCount(1);

    // Login
    currentUser.refresh(UserFactory.user());
    vm.inputs.loginSuccess();

    // The project should be starred, and a star prompt should be shown.
    starredTest.assertValues(false, false, true);
    showStarredPromptTest.assertValueCount(1);

    // A koala event should be tracked
    koalaTest.assertValues("Project Page", "Project Star");
  }

  @Test
  public void testProjectViewModel_StarProjectThatIsAlmostCompleted() {
    final Project project = ProjectFactory.almostCompletedProject();

    final CurrentUserType currentUser = new MockCurrentUser();
    final Environment environment = environment().toBuilder()
      .currentUser(currentUser)
      .build();

    final ProjectViewModel vm = new ProjectViewModel(environment);

    final TestSubscriber<Void> showStarredPromptTest = new TestSubscriber<>();
    vm.outputs.showStarredPrompt().subscribe(showStarredPromptTest);

    // Start the view model with an almost completed project
    vm.intent(new Intent().putExtra(IntentKey.PROJECT, project));

    // Login
    currentUser.refresh(UserFactory.user());

    // Star the project
    vm.inputs.starClicked();

    // The project should be starred, and a star prompt should NOT be shown.
    showStarredPromptTest.assertValueCount(0);
  }

  @Test
  public void testProjectViewModel_StarProjectThatIsSuccessful() {
    final Project project = ProjectFactory.successfulProject();

    final ApiClientType apiClient = new MockApiClient() {
      @Override
      public @NonNull
      Observable<Project> fetchProject(@NonNull String param) {
        return Observable.just(project);
      }
    };

    final CurrentUserType currentUser = new MockCurrentUser();
    final Environment environment = environment().toBuilder()
      .currentUser(currentUser)
      .apiClient(apiClient)
      .build();

    final ProjectViewModel vm = new ProjectViewModel(environment);

    final TestSubscriber<Void> showStarredPromptTest = new TestSubscriber<>();
    vm.outputs.showStarredPrompt().subscribe(showStarredPromptTest);

    // Start the view model with a successful project
    vm.intent(new Intent().putExtra(IntentKey.PROJECT, project));

    // Login
    currentUser.refresh(UserFactory.user());

    // Star the project
    vm.inputs.starClicked();

    // The project should be starred, and a star prompt should NOT be shown.
    showStarredPromptTest.assertValueCount(0);
  }
}
