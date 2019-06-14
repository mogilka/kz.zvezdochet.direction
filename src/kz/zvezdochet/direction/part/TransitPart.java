package kz.zvezdochet.direction.part;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.nebula.widgets.cdatetime.CDT;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import kz.zvezdochet.analytics.service.SphereService;
import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.decoration.InfoDecoration;
import kz.zvezdochet.core.ui.listener.ListSelectionListener;
import kz.zvezdochet.core.ui.provider.DictionaryLabelProvider;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.ui.view.ModelLabelProvider;
import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.direction.provider.TransitLabelProvider;
import kz.zvezdochet.part.Messages;
import kz.zvezdochet.provider.PlaceProposalProvider;
import kz.zvezdochet.provider.PlaceProposalProvider.PlaceContentProposal;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.service.HouseService;
import kz.zvezdochet.service.PlanetService;

/**
 * Представление транзитов события
 * @author Natalie Didenko
 *
 */
public class TransitPart extends ModelListView {
	@Inject
	public TransitPart() {}

	private Event person;
	private Place trplace;

	private CDateTime dt;
	private CDateTime dt2;
	private Text txPlace;
	private Text txLatitude;
	private Text txLongitude;
	private Text txZone;
	private Text txGreenwich;
	private ComboViewer cvPlanet;
	private ComboViewer cvHouse;
	private ComboViewer cvAspect;

	public void setPerson(Event person) {
		this.person = person;
	}
	
	@PostConstruct @Override
	public View create(Composite parent) {
		super.create(parent);
		return null;
	}

	@Override
	public boolean check(int mode) throws Exception {
		if (null == dt.getSelection())
			dt.setSelection(new Date());
		if (null == dt2.getSelection())
			dt2.setSelection(new Date(System.currentTimeMillis() + 84600));
		if (null == trplace)
			trplace = new Place().getDefault();
		if (txZone.getText().equals(""))
			txZone.setText("0.0");

		if (!DateUtil.isDateRangeValid(dt.getSelection(), dt2.getSelection())) {
			DialogUtil.alertError("Укажите правильный период");
			return false;
		} else if (null == person) {
			DialogUtil.alertError("Событие не задано");
			return false;
		}
		return true;
	}

	@Override
	protected void init(Composite parent) {
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().applyTo(container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
	}

	private Table table2;
	private CheckboxTableViewer tableViewer2;

	@Override
	public void initFilter(Composite parent) {
		grFilter = new Group(container, SWT.NONE);
		grFilter.setText("Период");

		Label lb = new Label(grFilter, SWT.NONE);
		lb.setText("Начало");
		dt = new CDateTime(grFilter, CDT.BORDER | CDT.COMPACT | CDT.DROP_DOWN | CDT.DATE_LONG | CDT.DATE_MEDIUM);
		dt.setNullText(""); //$NON-NLS-1$

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Конец");
		dt2 = new CDateTime(grFilter, CDT.BORDER | CDT.COMPACT | CDT.DROP_DOWN | CDT.DATE_LONG | CDT.DATE_MEDIUM);
		dt2.setNullText(""); //$NON-NLS-1$

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Сфера жизни");
		cvPlanet = new ComboViewer(grFilter, SWT.READ_ONLY | SWT.BORDER);
		cvHouse = new ComboViewer(grFilter, SWT.READ_ONLY | SWT.BORDER);

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Аспекты");
		cvAspect = new ComboViewer(grFilter, SWT.READ_ONLY | SWT.BORDER);

		tableViewer2 = CheckboxTableViewer.newCheckList(grFilter, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
		table2 = tableViewer2.getTable();
		table2.setHeaderVisible(false);
		table2.setLinesVisible(true);

		tableViewer2.setContentProvider(new ArrayContentProvider());
		tableViewer2.setLabelProvider(new ModelLabelProvider());
		
		ListSelectionListener listener = getSelectionListener();
		tableViewer2.addSelectionChangedListener(listener);
		tableViewer2.addDoubleClickListener(listener);

		Group secPlace = new Group(grFilter, SWT.NONE);
		secPlace.setText(Messages.getString("PersonView.Place")); //$NON-NLS-1$
		txPlace = new Text(secPlace, SWT.BORDER);
		new InfoDecoration(txPlace, SWT.TOP | SWT.LEFT);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Latitude")); //$NON-NLS-1$
		txLatitude = new Text(secPlace, SWT.BORDER);
		txLatitude.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Longitude")); //$NON-NLS-1$
		txLongitude = new Text(secPlace, SWT.BORDER);
		txLongitude.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Greenwith")); //$NON-NLS-1$
		txGreenwich = new Text(secPlace, SWT.BORDER);
		txGreenwich.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Zone")); //$NON-NLS-1$
		txZone = new Text(secPlace, SWT.BORDER);

		GridLayoutFactory.swtDefaults().numColumns(10).applyTo(grFilter);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grFilter);
		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(secPlace);
		GridDataFactory.fillDefaults().span(4, 1).grab(true, false).applyTo(secPlace);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(dt);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(dt2);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			span(4, 1).grab(true, false).applyTo(txPlace);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txLatitude);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txLongitude);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txZone);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txGreenwich);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvPlanet.getCombo());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvHouse.getCombo());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvAspect.getCombo());

		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, true).applyTo(table2);
	}

	/**
	 * Поиск персоны, для которой рассчитывается транзит
	 * @return персона
	 */
	public Event getPerson() {
		return person;
	}

	@Override
	public void reset() {
		dt.setNullText("");
		dt2.setNullText("");
		txPlace.setText(""); //$NON-NLS-1$
		txLatitude.setText(""); //$NON-NLS-1$
		txLongitude.setText(""); //$NON-NLS-1$
		txZone.setText(""); //$NON-NLS-1$
		txGreenwich.setText(""); //$NON-NLS-1$
	}

	public Date getInitialDate() {
		return dt.getSelection();
	}

	public Date getFinalDate() {
		return dt2.getSelection();
	}

	@Override
	public Model createModel() {
		return null;
	}

	@Override
	protected String[] initTableColumns() {
		return new String[] {
			"",
			"Транзитная планета",
			"Аспект",
			"Натальный объект",
			"Направление",
			"Величина аспекта",
			"Знак Зодиака",
			"Дом",
			"Описание"
		};
	}

	/**
	 * Инициализация представления местности события
	 * @param place местность
	 */
	private void initPlace(Place place) {
		if (null == place) return;
		txPlace.setText(place.getName());
		txLatitude.setText(CalcUtil.formatNumber("###.##", place.getLatitude())); //$NON-NLS-1$
		txLongitude.setText(CalcUtil.formatNumber("###.##", place.getLongitude())); //$NON-NLS-1$
		txGreenwich.setText(CalcUtil.formatNumber("###.##", place.getGreenwich())); //$NON-NLS-1$
		txZone.setText(String.valueOf(place.getGreenwich()));
	}

	/**
	 * Инициализация местностей
	 */
	private void setPlaces() {
	    PlaceProposalProvider proposalProvider = new PlaceProposalProvider();
	    ContentProposalAdapter adapter = new ContentProposalAdapter(
	        txPlace, new TextContentAdapter(),
	        proposalProvider, KeyStroke.getInstance(SWT.CTRL, 32), new char[] {' '});
	    adapter.setPropagateKeys(true);
	    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	    adapter.addContentProposalListener(new IContentProposalListener() {
			@Override
			public void proposalAccepted(IContentProposal proposal) {
				Place place = (Place)((PlaceContentProposal)proposal).getObject();
				if (place != null) {
					trplace = place;
					initPlace(place);
				}
			}
		});
	}

	@Override
	protected void initControls() {
		setPlaces();
		try {
			tableViewer2.setInput(new SphereService().getList());

			cvPlanet.setContentProvider(new ArrayContentProvider());
			cvPlanet.setLabelProvider(new DictionaryLabelProvider());
			List<Model> list = new PlanetService().getList();
			Planet planet = new Planet();
			planet.setId(0L);
			list.add(0, planet);
			cvPlanet.setInput(list);

			cvHouse.setContentProvider(new ArrayContentProvider());
			cvHouse.setLabelProvider(new DictionaryLabelProvider());
			list = new HouseService().getList();
			House house = new House();
			house.setId(0L);
			list.add(0, house);
			cvHouse.setInput(list);

			cvAspect.setContentProvider(new ArrayContentProvider());
			cvAspect.setLabelProvider(new DictionaryLabelProvider());
			list = new AspectService().getList();
			Aspect aspect = new Aspect();
			aspect.setId(0L);
			list.add(0, aspect);
			cvAspect.setInput(list);
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
	}

	public Place getPlace() {
		return trplace;
	}

	public double getZone() {
		return Double.parseDouble(txZone.getText());
	}

	public Object[] getSpheres() {
		return tableViewer2.getCheckedElements();
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new TransitLabelProvider();
	}

	/**
	 * Возвращает выбранную планету как сферу жизни
	 * @return планета
	 */
	public Planet getPlanet() {
		IStructuredSelection selection = (IStructuredSelection)cvPlanet.getSelection();
		if (selection.getFirstElement() != null) {
			Planet planet = (Planet)selection.getFirstElement();
			if (planet.getId() > 0)
				return planet;
		}
		return null;
	}

	/**
	 * Возвращает выбранный дом как сферу жизни
	 * @return астрологический дом
	 */
	public House getHouse() {
		IStructuredSelection selection = (IStructuredSelection)cvHouse.getSelection();
		if (selection.getFirstElement() != null) {
			House house = (House)selection.getFirstElement();
			if (house.getId() > 0)
				return house;
		}
		return null;
	}

	/**
	 * Возвращает выбранный аспект
	 * @return аспект
	 */
	public Aspect getAspect() {
		IStructuredSelection selection = (IStructuredSelection)cvAspect.getSelection();
		if (selection.getFirstElement() != null) {
			Aspect type = (Aspect)selection.getFirstElement();
			if (type.getId() > 0)
				return type;
		}
		return null;
	}
}
