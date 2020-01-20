package it.tidal.climax.ui.views.dashboard;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.starter.beveragebuddy.ui.MainLayout;

@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Dashoboard")
public class Dashboard extends VerticalLayout {

    private final H2 header = new H2("Dashboard");

    public Dashboard() {

        initView();
        addContent();
    }

    private void initView() {

        addClassName("dashboard");
        setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
    }

    private void addContent() {

        VerticalLayout container = new VerticalLayout();
        container.setClassName("view-container");
        container.setAlignItems(Alignment.STRETCH);

        container.add(header);
        add(container);
    }
}
