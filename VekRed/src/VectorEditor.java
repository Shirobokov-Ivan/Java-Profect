import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class VectorEditor extends JFrame {
    private ArrayList<Point> points = new ArrayList<>();
    private ArrayList<Edge> edges = new ArrayList<>();
    private String currentShape = "Point";
    private int selectedIndex = -1;
    private ArrayList<Point> selectedPoints = new ArrayList<>();
    private Point initialClickPoint;
    private DrawingPanel drawingPanel;
    private boolean showEdges = false;
    private Color lineColor = Color.BLACK;
    private float lineWidth = 2.0f;
    private boolean isFlying = false;
    private int[] velocitiesX;
    private int[] velocitiesY;
    private boolean showGrid = false;

    public VectorEditor() {
        setTitle("Векторный Редактор");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        JMenuBar menuBar = new JMenuBar();
        JMenu shapeMenu = new JMenu("Фигуры");
        JMenuItem squareMenuItem = new JMenuItem("Квадрат");
        JMenuItem triangleMenuItem = new JMenuItem("Треугольник");
        squareMenuItem.addActionListener(e -> {
            currentShape = "Square";
            createSquare();
        });
        triangleMenuItem.addActionListener(e -> {
            currentShape = "Triangle";
            createTriangle();
        });
        shapeMenu.add(squareMenuItem);
        shapeMenu.add(triangleMenuItem);
        menuBar.add(shapeMenu);

        JMenu pointMenu = new JMenu("Точки");
        JMenuItem pointButton = new JMenuItem("Точка");
        pointButton.addActionListener(e -> currentShape = "Point");
        pointMenu.add(pointButton);

        JMenuItem insertPointButton = new JMenuItem("Вставить Точку");
        insertPointButton.addActionListener(e -> currentShape = "InsertPoint");
        pointMenu.add(insertPointButton);
        menuBar.add(pointMenu);

        JMenu moveMenu = new JMenu("Перемещение");
        JMenuItem moveButton = new JMenuItem("Переместить Точку");
        moveButton.addActionListener(e -> currentShape = "Move");
        moveMenu.add(moveButton);

        JMenuItem moveFigureButton = new JMenuItem("Переместить фигуру");
        moveFigureButton.addActionListener(e -> currentShape = "MoveFigure");
        moveMenu.add(moveFigureButton);
        menuBar.add(moveMenu);

        JMenu functionMenu = new JMenu("Функции");
        JButton showEdgesButton = new JButton("Показать рёбра");
        showEdgesButton.addActionListener(e -> {
            showEdges = !showEdges;
            drawingPanel.repaint();
        });
        functionMenu.add(showEdgesButton);

        JButton flyPointsButton = new JButton("Полёт точек");
        flyPointsButton.addActionListener(e -> toggleFlyPoints());
        functionMenu.add(flyPointsButton);

        JButton gridButton = new JButton("Сетка");
        gridButton.addActionListener(e -> {
            showGrid = !showGrid;
            drawingPanel.repaint();
        });
        functionMenu.add(gridButton);

        menuBar.add(functionMenu);
        setJMenuBar(menuBar);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        JButton clearButton = new JButton("Очистить");
        clearButton.addActionListener(e -> {
            points.clear();
            edges.clear();
            drawingPanel.repaint();
            isFlying = false;
        });

        buttonPanel.add(clearButton);

        String[] colors = {"Чёрный", "Красный", "Зелёный", "Синий"};
        JComboBox<String> colorSelector = new JComboBox<>(colors);

        colorSelector.addActionListener(e -> {
            switch ((String) colorSelector.getSelectedItem()) {
                case "Чёрный": lineColor = Color.BLACK; break;
                case "Красный": lineColor = Color.RED; break;
                case "Зелёный": lineColor = Color.GREEN; break;
                case "Синий": lineColor = Color.BLUE; break;
            }
            drawingPanel.repaint();
        });

        buttonPanel.add(colorSelector);

        String[] widths = {"1", "2", "3", "4", "5"};
        JComboBox<String> widthSelector = new JComboBox<>(widths);

        widthSelector.addActionListener(e -> {
            lineWidth = Float.parseFloat((String) widthSelector.getSelectedItem());
            drawingPanel.repaint();
        });

        buttonPanel.add(widthSelector);

        drawingPanel = new DrawingPanel();
        drawingPanel.setBackground(Color.WHITE);

        add(buttonPanel, BorderLayout.NORTH);
        add(drawingPanel, BorderLayout.CENTER);

        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentShape.equals("Point")) {
                    points.add(e.getPoint());
                    if (points.size() > 1) {
                        edges.add(new Edge(points.get(points.size() - 2), points.get(points.size() - 1)));
                    }
                    drawingPanel.repaint();
                } else if (currentShape.equals("InsertPoint") && points.size() > 2) {
                    Point newPoint = e.getPoint();
                    int[] closestIndices = findTwoClosestPoints(newPoint);
                    if (closestIndices[0] != -1 && closestIndices[1] != -1) {
                        edges.removeIf(edge -> (edge.start.equals(points.get(closestIndices[0])) && edge.end.equals(points.get(closestIndices[1]))) ||
                                (edge.start.equals(points.get(closestIndices[1])) && edge.end.equals(points.get(closestIndices[0]))));
                        points.add(newPoint);
                        edges.add(new Edge(points.get(closestIndices[0]), newPoint));
                        edges.add(new Edge(newPoint, points.get(closestIndices[1])));
                        drawingPanel.repaint();
                    }
                } else if (currentShape.equals("MoveFigure")) {
                    selectAllPoints();
                    initialClickPoint=e.getPoint();
                } else if (currentShape.equals("Move")) {
                    selectedIndex=findClosestPoint(e.getPoint());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!selectedPoints.isEmpty()) {
                    Point delta=new Point(e.getX()-initialClickPoint.x,e.getY()-initialClickPoint.y);
                    for(Point p:selectedPoints) {
                        int index=points.indexOf(p);
                        if(index!=-1) {
                            points.set(index,new Point(p.x+delta.x,p.y+delta.y));
                        }
                    }
                    selectedPoints.clear();
                    updateEdges();
                    drawingPanel.repaint();
                } else if(selectedIndex!=-1) {
                    points.set(selectedIndex,e.getPoint());
                    selectedIndex=-1;
                    updateEdges();
                    drawingPanel.repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!selectedPoints.isEmpty()) {
                    Point delta=new Point(e.getX()-initialClickPoint.x,e.getY()-initialClickPoint.y);
                    for(Point p:selectedPoints) {
                        int index=points.indexOf(p);
                        if(index!=-1) {
                            points.set(index,new Point(p.x+delta.x,p.y+delta.y));
                        }
                    }
                    updateEdges();
                    drawingPanel.repaint();
                } else if (selectedIndex != -1) {
                    points.set(selectedIndex, e.getPoint());
                    updateEdges();
                    drawingPanel.repaint();
                }
            }

        });

    }

    private void toggleFlyPoints() {
        isFlying=!isFlying;
        if(isFlying) {
            velocitiesX=new int[points.size()];
            velocitiesY=new int[points.size()];
            for(int i=0;i<velocitiesX.length;i++) {
                velocitiesX[i]=(Math.random()<0.5?1:-1)*(int)(Math.random()*5+1);
                velocitiesY[i]=(Math.random()<0.5?1:-1)*(int)(Math.random()*5+1);
            }
            flyPoints();
        }
    }

    private void flyPoints() {
        Timer timer=new Timer(30,e->{
            for(int i=0;i<points.size();i++) {
                Point p=points.get(i);
                p.x+=velocitiesX[i];
                p.y+=velocitiesY[i];
                if(p.x<0||p.x>getWidth()) velocitiesX[i]*=-1;
                if(p.y<0||p.y>getHeight()) velocitiesY[i]*=-1;
                points.set(i,p);
            }
            drawingPanel.repaint();
            if(!isFlying)((Timer)e.getSource()).stop();
        });
        timer.start();
    }

    private int findClosestPoint(Point p) {
        int closestIndex=-1;
        double closestDistance=Double.MAX_VALUE;
        for(int i=0;i<points.size();i++) {
            double distance=p.distance(points.get(i));
            if(distance<closestDistance) { closestDistance=distance; closestIndex=i; }
        } return closestIndex;
    }

    private int[] findTwoClosestPoints(Point p) {
        int[] closestIndices=new int[2];
        double[] closestDistances={Double.MAX_VALUE,Double.MAX_VALUE};
        for(int i=0;i<points.size();i++) {
            double distance=p.distance(points.get(i));
            if(distance<closestDistances[0])
            { closestDistances[1]=closestDistances[0];
                closestIndices[1]=closestIndices[0];
                closestDistances[0]=distance; closestIndices[0]=i;
            }
            else if(distance<closestDistances[1])
            { closestDistances[1]=distance;
                closestIndices[1]=i; }
        } return closestIndices;
    }

    private void selectAllPoints()
    {
        selectedPoints.clear();
        selectedPoints.addAll(points);
    }

    private void updateEdges()
    {
        edges.clear();
        for(int i=0;i<points.size()-1;i++)
            edges.add(new Edge(points.get(i),points.get(i+1)));
    }

    private void createSquare()
    {
        if(points.size()>=4)return;
        points.add(new Point(100,100));
        points.add(new Point(200,100));
        points.add(new Point(200,200));
        points.add(new Point(100,200));
        edges.add(new Edge(points.get(0),points.get(1)));
        edges.add(new Edge(points.get(1),points.get(2)));
        edges.add(new Edge(points.get(2),points.get(3)));
        edges.add(new Edge(points.get(3),points.get(0)));
        drawingPanel.repaint(); }

    private void createTriangle()
    {
        if(points.size()>=3)return;
        points.add(new Point(100,100));
        points.add(new Point(200,100));
        points.add(new Point(150,200));
        edges.add(new Edge(points.get(0),points.get(1)));
        edges.add(new Edge(points.get(1),points.get(2)));
        edges.add(new Edge(points.get(2),points.get(0)));
        drawingPanel.repaint(); }

    private class DrawingPanel extends JPanel {
        @Override protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            if (showGrid)
            {
                g.setColor(Color.LIGHT_GRAY);
                for (int i=0;i<getWidth();i+=20)
                    g.drawLine(i,0,i,getHeight());
                for(int j=0;j<getHeight();j+=20)
                    g.drawLine(0,j,getWidth(),j); } g.setColor(Color.RED);
            for(int i=0;i<points.size();i++)
            {
                Point p=points.get(i);
                g.fillOval(p.x-5,p.y-5,10,10);
                g.setColor(Color.BLACK);
                g.drawString(Integer.toString(i),p.x+7,p.y); g.setColor(Color.RED);
            }
            if(showEdges)
            {
                g.setColor(lineColor);
                Graphics2D g2d=(Graphics2D)g;
                g2d.setStroke(new BasicStroke(lineWidth));
                for(Edge edge:edges) g.drawLine(edge.start.x,edge.start.y,edge.end.x,edge.end.y);
            }
        }
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> { VectorEditor editor = new VectorEditor();
            editor.setVisible(true); });
    }
}

class Edge {
    Point start, end;

    public Edge(Point start, Point end) {
        this.start = start;
        this.end = end;
    }
}