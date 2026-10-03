/**
 * data.js — Mock de arrendatarios hardcodeado (fuente de datos v1).
 * Expone: window.RentalsData (array de Tenant).
 *
 * Tres casos de prueba:
 *   t-001: Al día (todos los meses pagados hasta 2026-10).
 *   t-002: Atrasado 2 meses (últimos dos meses sin pagar).
 *   t-003: Atrasado con un cargo único pendiente (daño al departamento).
 */

window.RentalsData = [
  {
    id: "t-001",
    fullName: "María Pérez",
    unit: "Depto 301",
    contact: "maria.perez@mail.com",
    paymentDay: 5,
    startDate: "2026-07-01",
    monthlyAmount: 450,
    oneTimeCharges: [
      { id: "c-001", description: "Garantía", amount: 450, paid: true }
    ],
    paidMonths: ["2026-07", "2026-08", "2026-09", "2026-10"]
  },
  {
    id: "t-002",
    fullName: "Carlos Ramos",
    unit: "Depto 302",
    contact: "+56 9 1234 5678",
    paymentDay: 10,
    startDate: "2026-06-01",
    monthlyAmount: 500,
    oneTimeCharges: [
      { id: "c-002", description: "Garantía", amount: 500, paid: true }
    ],
    paidMonths: ["2026-06", "2026-07", "2026-08"]
  },
  {
    id: "t-003",
    fullName: "Ana Torres",
    unit: "Depto 201",
    contact: "ana.torres@mail.com",
    paymentDay: 1,
    startDate: "2026-08-01",
    monthlyAmount: 400,
    oneTimeCharges: [
      { id: "c-003", description: "Garantía", amount: 400, paid: true },
      { id: "c-004", description: "Reparación ventana", amount: 120, paid: false }
    ],
    paidMonths: ["2026-08", "2026-09", "2026-10"]
  }
];
