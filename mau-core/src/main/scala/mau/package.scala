package object mau {
  type Id = String
  type Key = String
  type KeyFunction[A] = Function1[A, Set[Key]]
  type FilterFunction[A] = Function1[A, Boolean]
}
